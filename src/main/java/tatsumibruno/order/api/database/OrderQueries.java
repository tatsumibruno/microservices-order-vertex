package tatsumibruno.order.api.database;

import graphql.com.google.common.base.Preconditions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import tatsumibruno.order.api.commons.handlers.DatabaseHandler;
import tatsumibruno.order.api.commons.handlers.DatabaseTransactionHandler;
import tatsumibruno.order.api.domain.OrderCustomer;
import tatsumibruno.order.api.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public enum OrderQueries {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderQueries.class);
    private static final String SQL_INSERT_ORDER = """
            INSERT INTO orders
            (id, code, status, created_at, customer_name, customer_email, delivery_address)
            VALUES(nextval('orders_seq'), $1, $2, $3, $4, $5, $6)
            """;
    private static final String SQL_FIND_BY_CODE = "SELECT id, code, customer_name, customer_email, delivery_address, status, created_at FROM orders WHERE code = $1";

    public Future<Void> insert(OrderDBModel newOrder) {
        DatabaseTransactionHandler transaction = DatabaseHandler.INSTANCE.initTransaction();
        OrderCustomer customer = newOrder.getCustomer();
        transaction.addOperation(SQL_INSERT_ORDER, List.of(newOrder.getCode(),
                OrderStatus.OPENED,
                LocalDateTime.now(),
                customer.name(),
                customer.email(),
                customer.deliveryAddress()));
        return transaction.execute();
    }

    public Future<OrderDBModel> findByCode(UUID requestCode) {
        final Promise<OrderDBModel> promise = Promise.promise();
        DatabaseHandler.INSTANCE
                .executeQuery(SQL_FIND_BY_CODE, List.of(requestCode))
                .onSuccess(result -> {
                    Preconditions.checkState(result.rowCount() <= 1, "Should have at least 1 order with each code");
                    result.forEach(row -> {
                        final Long id = row.getLong("id");
                        final UUID code = row.getUUID("code");
                        final String customerName = row.getString("customer_name");
                        final String customerEmail = row.getString("customer_email");
                        final String deliveryAddress = row.getString("delivery_address");
                        final OrderCustomer customer = new OrderCustomer(customerName, customerEmail, deliveryAddress);
                        promise.complete(new OrderDBModel(code, customer)
                                .setId(id)
                                .setCreatedAt(row.getLocalDateTime("created_at").atZone(TimeZone.getDefault().toZoneId()))
                                .setStatus(row.get(OrderStatus.class, "status"))
                        );
                    });
                    promise.fail("Order with code " + requestCode + " doest not exists");
                })
                .onFailure(failure -> {
                    LOGGER.error("Error while executing query " + SQL_FIND_BY_CODE, failure);
                    promise.fail(failure);
                });
        return promise.future();
    }
}

;
