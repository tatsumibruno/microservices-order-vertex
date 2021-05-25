package tatsumibruno.order.commons.handlers;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public interface InfrastructureHandler {

  Future<Void> register(Vertx vertx);

  void closeResources();
}
