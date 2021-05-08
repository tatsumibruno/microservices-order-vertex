package tatsumibruno.order.api.create_order;

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
import tatsumibruno.order.api.commons.ErrorResponse;
import tatsumibruno.order.api.commons.handlers.ApiHandler;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public enum CreateOrderApiHandler implements ApiHandler {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrderApiHandler.class);

  @Override
  public void register(Vertx vertx, Router router) {
    LOGGER.info("Registering CreateOrderApiHandler...");
    KafkaProducer<String, String> producer = kafkaProducer(vertx);
    router.post("/orders")
      .handler(getValidationHandler(vertx))
      .handler(ctx -> {
        LOGGER.info("Recebendo novo pedido");
        HttpServerResponse response = ctx.response();
        OrderRequest orderRequest = Json.decodeValue(ctx.getBodyAsString(), OrderRequest.class);
        CreatedOrder createdOrder = new CreatedOrder(UUID.randomUUID(),
          ZonedDateTime.now(),
          orderRequest.getCustomerName(),
          orderRequest.getCustomerEmail(),
          orderRequest.getDeliveryAddress());
        createdOrder.save()
          .onSuccess(unused -> {
            LOGGER.info("Pedido criado no banco com sucesso, agora será enviado ao tópico new-orders. " + createdOrder);
            producer.send(KafkaProducerRecord.create("new-orders",
              createdOrder.getCode().toString(),
              Json.encode(createdOrder)));
            response.end(Json.encode(createdOrder));
          })
          .onFailure(failure -> {
            LOGGER.info("Erro ao inserir pedido no banco. " + createdOrder);
            response.setStatusCode(400);
            response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
          });
      });
  }

  private KafkaProducer<String, String> kafkaProducer(Vertx vertx) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");
    return KafkaProducer.create(vertx, config);
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
}
