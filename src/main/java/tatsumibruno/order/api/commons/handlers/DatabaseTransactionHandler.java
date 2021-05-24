package tatsumibruno.order.api.commons.handlers;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class DatabaseTransactionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTransactionHandler.class);

    private final PgPool databasePool;
    private final List<DatabaseTransactionOperation> operations = new ArrayList<>();

    DatabaseTransactionHandler(PgPool databasePool) {
        this.databasePool = databasePool;
    }

    public DatabaseTransactionHandler addOperation(String query, List<Object> params) {
        operations.add(new DatabaseTransactionOperation(query, params));
        return this;
    }

    public Future<Void> execute() {
        return Future.future(resultHandler -> databasePool
                .getConnection()
                .onFailure(error -> {
                    LOGGER.error("Error on executing transaction with " + operations.size() + " operations", error);
                    resultHandler.fail(error);
                })
                .onSuccess(connection -> connection.begin()
                        .onSuccess(transaction -> executeOperations(transaction, connection)
                                .onSuccess($ -> connection.close()
                                        .onSuccess(closeConnectionHandler -> {
                                            LOGGER.info("Transaction with " + operations.size() + " operations completed");
                                            resultHandler.complete();
                                        })
                                        .onFailure(failure -> {
                                            LOGGER.error("Error while closing the connection", failure);
                                            resultHandler.fail(failure);
                                        })
                                )
                                .onFailure(failure -> {
                                    LOGGER.error("Error while executing the transaction", failure);
                                    resultHandler.fail(failure);
                                })
                        )
                        .onFailure(failure -> {
                            LOGGER.error("Error while initializing the transaction", failure);
                            resultHandler.fail(failure);
                        })));
    }

    private Future<Void> executeOperations(Transaction transaction, SqlConnection connection) {
        return Future.future(resultHandler -> {
            List<Future> operationsFutures = new ArrayList<>();
            for (DatabaseTransactionOperation operation : operations) {
                operationsFutures.add(Future.future(handler -> connection.preparedQuery(operation.query())
                        .execute(Tuple.from(operation.params()))
                        .onSuccess($ -> handler.complete())
                        .onFailure(failure -> {
                            LOGGER.error("Error on executing query " + operation.query(), failure);
                            handler.fail(failure);
                        })));
            }
            CompositeFuture.all(operationsFutures)
                    .onSuccess($ -> transaction.commit(handler -> {
                        if (handler.failed()) {
                            LOGGER.error("Error while commiting the transaction", handler.cause());
                            resultHandler.fail(handler.cause());
                        } else {
                            resultHandler.complete();
                        }
                    }))
                    .onFailure(failure -> transaction.rollback($ -> resultHandler.fail("Rolling back transaction because some query has errors")));
        });
    }
}
