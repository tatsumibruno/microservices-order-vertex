package tatsumibruno.order.api.query;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import tatsumibruno.order.api.commons.ErrorResponse;
import tatsumibruno.order.api.commons.handlers.ApiHandler;
import tatsumibruno.order.api.database.OrderRepository;
import tatsumibruno.order.api.domain.OrderCustomer;

import java.util.UUID;

public enum QueryOrderApiHandler implements ApiHandler {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryOrderApiHandler.class);

    @Override
    public void register(Vertx vertx, Router router) {
        LOGGER.info("Registering QueryOrderApiHandler...");
        router.get("/orders/:code")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    String code = ctx.pathParam("code");
                    LOGGER.info("Searching for order " + code);
                    OrderRepository.INSTANCE.findByCode(UUID.fromString(code))
                            .onSuccess(orderDBModel -> {
                                final OrderCustomer customer = orderDBModel.getCustomer();
                                OrderResponse createdOrder = new OrderResponse(
                                        orderDBModel.getCode(),
                                        orderDBModel.getCreatedAt(),
                                        customer.name(),
                                        customer.email(),
                                        customer.deliveryAddress()
                                );
                                response.end(Json.encode(createdOrder));
                            })
                            .onFailure(failure -> {
                                LOGGER.error(failure.getMessage());
                                response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
                            });
                });
    }
}
