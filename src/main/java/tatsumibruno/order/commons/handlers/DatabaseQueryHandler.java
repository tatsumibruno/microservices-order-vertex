package tatsumibruno.order.commons.handlers;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class DatabaseQueryHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseQueryHandler.class);

    private final PgPool databasePool;

    Future<RowSet<Row>> execute(String query, List<Object> params) {
        return Future.future(resultHandler -> databasePool
                .getConnection()
                .onFailure(error -> {
                    LOGGER.error("Error on getting connection " + query, error);
                    resultHandler.fail(error);
                })
                .onSuccess(connection -> connection
                        .preparedQuery(query)
                        .execute(Tuple.from(params))
                        .onSuccess(queryHandler -> {
                            RowSet<Row> resultSet = queryHandler.value();
                            connection.close()
                                    .onSuccess(closeHandler -> resultHandler.complete(resultSet))
                                    .onFailure(failure -> {
                                        LOGGER.error("Error while closing the connection.", failure);
                                        resultHandler.fail(failure);
                                    });
                        })
                        .onFailure(failure -> {
                            LOGGER.error("Error while executing query: " + query, failure);
                            resultHandler.fail(failure);
                        })
                ));
    }
}
