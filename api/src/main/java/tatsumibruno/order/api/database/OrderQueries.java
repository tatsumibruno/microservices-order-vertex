package tatsumibruno.order.api.database;

import io.vertx.core.Future;
import tatsumibruno.order.api.commons.handlers.DatabaseHandler;
import tatsumibruno.order.api.commons.handlers.DatabaseTransactionHandler;
import tatsumibruno.order.api.domain.OrderCustomer;
import tatsumibruno.order.api.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public enum OrderQueries {

  INSTANCE;

  private static final String SQL_INSERT_ORDER = """
    INSERT INTO orders
    (id, code, status, created_at, customer_name, customer_email, delivery_address)
    VALUES(nextval('orders_seq'), $1, $2, $3, $4, $5, $6)
    """;

  public Future<Void> insert(OrderDBModel newOrder) {
    DatabaseTransactionHandler transaction = DatabaseHandler.INSTANCE.initTransaction();
    OrderCustomer customer = newOrder.customer();
    transaction.addOperation(SQL_INSERT_ORDER, List.of(newOrder.code(),
      OrderStatus.OPENED,
      LocalDateTime.now(),
      customer.name(),
      customer.email(),
      customer.deliveryAddress()));
    return transaction.execute();
  }
}

;
