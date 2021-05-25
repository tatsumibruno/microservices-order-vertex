package tatsumibruno.order.commons.handlers;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public interface ApiHandler {

  void register(Vertx vertx, Router router);
}
