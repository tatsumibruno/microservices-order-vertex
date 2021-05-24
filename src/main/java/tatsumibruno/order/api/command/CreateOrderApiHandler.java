package tatsumibruno.order.api.command;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.impl.body.JsonBodyProcessorImpl;
import io.vertx.ext.web.validation.impl.validator.SchemaValidator;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.Keywords;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import lombok.AllArgsConstructor;
import tatsumibruno.order.api.commons.ErrorResponse;
import tatsumibruno.order.api.commons.handlers.ApiHandler;
import tatsumibruno.order.api.commons.infra.KafkaUtils;
import tatsumibruno.order.api.database.OrderRepository;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

@AllArgsConstructor
public enum CreateOrderApiHandler implements ApiHandler {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrderApiHandler.class);

    @Override
    public void register(Vertx vertx, Router router) {
        LOGGER.info("Registering CreateOrderApiHandler...");
        KafkaProducer<String, String> producer = KafkaUtils.producer(vertx);
        router.post("/orders")
                .handler(getValidationHandler(vertx))
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    String bodyRequest = ctx.getBodyAsString();
                    LOGGER.info("Receiving new order: " + bodyRequest);
                    OrderRequest orderRequest = Json.decodeValue(bodyRequest, OrderRequest.class);
                    CreatedOrder createdOrder = new CreatedOrder(UUID.randomUUID(), ZonedDateTime.now(), orderRequest.toOrderCustomer());
                    persistOrder(producer, response, createdOrder);
                });
    }

    private ValidationHandler getValidationHandler(Vertx vertx) {
        SchemaParser parser = SchemaParser.createOpenAPI3SchemaParser(
                SchemaRouter.create(vertx, new SchemaRouterOptions())
        );
        Schema schema = objectSchema()
                .requiredProperty("customerName", stringSchema().with(Keywords.minLength(5)).alias("customerName"))
                .requiredProperty("customerEmail", stringSchema().with(Keywords.pattern(Pattern.compile("(.*)\\@(.*)"))).alias("customerEmail"))
                .build(parser);
        return ValidationHandler
                .builder(parser)
                .body(new JsonBodyProcessorImpl(new SchemaValidator(schema)))
                .build();
    }

    private void persistOrder(KafkaProducer<String, String> producer, HttpServerResponse response, CreatedOrder createdOrder) {
        OrderRepository.INSTANCE.insert(createdOrder.toDBModel())
                .onSuccess(unused -> {
                    LOGGER.info("Order created on database, sending to topic 'orders-created'. " + createdOrder);
                    producer.send(KafkaProducerRecord.create("orders-created",
                            createdOrder.getCode().toString(),
                            Json.encode(createdOrder)));
                    response.end(Json.encode(createdOrder));
                })
                .onFailure(failure -> {
                    LOGGER.info("Error while persist order on database: " + createdOrder);
                    response.setStatusCode(400);
                    response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
                });
    }
}
