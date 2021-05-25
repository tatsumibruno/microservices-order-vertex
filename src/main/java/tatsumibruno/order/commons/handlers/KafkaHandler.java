package tatsumibruno.order.commons.handlers;

import io.vertx.core.Vertx;

public interface KafkaHandler {

  void register(Vertx vertx);
}
