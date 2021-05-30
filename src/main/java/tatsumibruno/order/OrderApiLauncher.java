package tatsumibruno.order;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import tatsumibruno.order.command.CreateOrderApiHandler;
import tatsumibruno.order.command.OrdersStatusChangesKafkaHandler;
import tatsumibruno.order.commons.ErrorResponse;
import tatsumibruno.order.commons.handlers.DatabaseHandler;
import tatsumibruno.order.query.QueryOrderApiHandler;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class OrderApiLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderApiLauncher.class);

  public static void main(String[] args) {
    Launcher.executeCommand("run", LauncherVerticle.class.getName());
  }

  public static class LauncherVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startFuture) {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      configureObjectMapper();
      registerInfrastructureHandlers();
      registerKafkaHandlers();
      Router router = registerApiRouter();
      HttpServer httpServer = vertx.createHttpServer();
      httpServer.requestHandler(router).listen(8000);
    }

    @Override
    public void stop() {
      DatabaseHandler.INSTANCE.closeResources();
    }

    private void registerInfrastructureHandlers() {
      DatabaseHandler.INSTANCE.register(vertx);
    }

    private void registerKafkaHandlers() {
      OrdersStatusChangesKafkaHandler.INSTANCE.register(vertx);
    }

    private Router registerApiRouter() {
      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      CreateOrderApiHandler.INSTANCE.register(vertx, router);
      QueryOrderApiHandler.INSTANCE.register(vertx, router);
      registerErrorRoutes(router);
      return router;
    }

    private void registerErrorRoutes(Router router) {
      router.errorHandler(500, routingContext -> {
        Throwable failure = routingContext.failure();
        LOGGER.error("Internal error while processing request.", failure);
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(500);
        response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
      });
      router.errorHandler(400, routingContext -> {
        Throwable failure = routingContext.failure();
        LOGGER.error("Validation error while processing request.", failure);
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(400);
        response.end(Json.encode(ErrorResponse.of(failure, failure.getMessage())));
      });
    }
  }

  private static void configureObjectMapper() {
    final ObjectMapper objectMapper = DatabindCodec.mapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setDateFormat(new StdDateFormat());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
}
