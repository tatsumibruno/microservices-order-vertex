package tatsumibruno.order.api.commons.handlers;

import io.vertx.core.Vertx;

public interface KafkaHandler {

  void register(Vertx vertx);
}
