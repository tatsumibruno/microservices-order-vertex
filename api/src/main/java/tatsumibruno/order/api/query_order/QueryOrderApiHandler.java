package tatsumibruno.order.api.query_order;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import tatsumibruno.order.api.commons.ErrorResponse;
import tatsumibruno.order.api.commons.handlers.ApiHandler;
import tatsumibruno.order.api.commons.handlers.DatabaseHandler;

import java.util.List;
import java.util.TimeZone;

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
        DatabaseHandler.INSTANCE.executeQuery("SELECT * FROM ORDERS WHERE CODE = $1", List.of(code))
          .onSuccess(rowSetHandler -> {
            if (rowSetHandler.size() == 0) {
              response.setStatusCode(404).end(Json.encode(ErrorResponse.of("NotFound", "Order not found")));
              return;
            }
            rowSetHandler.forEach(row -> {
              OrderResponse createdOrder = new OrderResponse(
                row.getUUID("code"),
                row.getLocalDateTime("created_at").atZone(TimeZone.getDefault().toZoneId()),
                row.getString("customer_name"),
                row.getString("customer_email"),
                row.getString("delivery_address")
              );
              response.end(Json.encode(createdOrder));
            });
          })
          .onFailure(failure -> {
            LOGGER.error("Error while process endpoint GET /orders/:code: ", failure);
            response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
          });
      });
  }
}
