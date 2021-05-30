package tatsumibruno.order.infra;

import graphql.com.google.common.base.Preconditions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import lombok.NonNull;
import tatsumibruno.order.commons.handlers.DatabaseHandler;
import tatsumibruno.order.commons.handlers.DatabaseTransactionHandler;
import tatsumibruno.order.domain.Order;
import tatsumibruno.order.domain.OrderCustomer;
import tatsumibruno.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public enum OrderRepository {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRepository.class);
    private static final String SQL_INSERT_ORDER = """
            INSERT INTO orders
            (id, code, status, created_at, customer_name, customer_email, delivery_address)
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            """;
    private static final String SQL_FIND_BY_CODE = """
            SELECT id, code, customer_name, customer_email, delivery_address, status, created_at 
            FROM orders 
            WHERE code = $1
            """;
    private static final String SQL_INSERT_ORDER_HISTORY = """
            INSERT INTO orders_history 
            (id, orders_id, status, created_at) 
            VALUES (nextval('orders_history_seq'), $1, $2, $3)
            """;

    public Future<Void> insert(Order newOrder) {
        return DatabaseHandler.INSTANCE
                .executeQuery("select nextval('orders_seq')", List.of())
                .map(rowSet -> {
                    AtomicLong orderId = new AtomicLong();
                    rowSet.forEach(row -> orderId.set(row.getLong("nextval")));
                    return orderId;
                })
                .compose(orderId -> {
                    DatabaseTransactionHandler transaction = DatabaseHandler.INSTANCE.initTransaction();
                    OrderCustomer customer = newOrder.getCustomer();
                    transaction.addOperation(SQL_INSERT_ORDER, List.of(orderId.get(),
                            newOrder.getCode(),
                            OrderStatus.OPENED,
                            LocalDateTime.now(),
                            customer.name(),
                            customer.email(),
                            customer.deliveryAddress()));
                    transaction.addOperation(SQL_INSERT_ORDER_HISTORY, List.of(orderId.get(),
                            OrderStatus.OPENED,
                            LocalDateTime.now()));
                    return transaction.execute();
                });
    }

    public Future<Order> findByCode(UUID requestCode) {
        final Promise<Order> promise = Promise.promise();
        DatabaseHandler.INSTANCE
                .executeQuery(SQL_FIND_BY_CODE, List.of(requestCode))
                .onSuccess(result -> {
                    Preconditions.checkState(result.rowCount() <= 1, "Should have at least 1 order with each code");
                    if (result.rowCount() == 0) {
                        promise.fail("Order with code " + requestCode + " doest not exists");
                        return;
                    }
                    result.forEach(row -> {
                        final Long id = row.getLong("id");
                        final UUID code = row.getUUID("code");
                        final String customerName = row.getString("customer_name");
                        final String customerEmail = row.getString("customer_email");
                        final String deliveryAddress = row.getString("delivery_address");
                        final OrderCustomer customer = new OrderCustomer(customerName, customerEmail, deliveryAddress);
                        promise.complete(new Order(code, customer)
                                .setId(id)
                                .setCreatedAt(row.getLocalDateTime("created_at").atZone(TimeZone.getDefault().toZoneId()))
                                .setStatus(row.get(OrderStatus.class, "status"))
                        );
                    });
                })
                .onFailure(failure -> {
                    LOGGER.error("Error while executing query " + SQL_FIND_BY_CODE, failure);
                    promise.fail(failure);
                });
        return promise.future();
    }

    public Future<Void> updateStatus(@NonNull Long orderId, @NonNull OrderStatus status, @NonNull ZonedDateTime modifiedAt) {
        return DatabaseHandler.INSTANCE
                .initTransaction()
                .addOperation("UPDATE orders SET status = $1 WHERE id = $2",
                        List.of(status, orderId))
                .addOperation(SQL_INSERT_ORDER_HISTORY,
                        List.of(orderId, status, modifiedAt.toLocalDateTime()))
                .execute();
    }
}
