package tatsumibruno.order.api.commons.handlers.db;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import tatsumibruno.order.api.commons.handlers.InfrastructureHandler;

import java.util.List;

import static java.util.Objects.nonNull;

public enum DatabaseHandler implements InfrastructureHandler {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);

  private static final DatabaseConfiguration CONFIGURATION = new DatabaseConfiguration(
    System.getProperty("db_host", "localhost"),
    Integer.valueOf(System.getProperty("db_port", "5432")),
    System.getProperty("db_name", "orders_api"),
    System.getProperty("db_user", "postgres"),
    System.getProperty("db_password", "postgres")
  );

  private PgPool DB_POOL;

  @Override
  public Future<Void> register(Vertx vertx) {
    LOGGER.info("Registering DatabaseSetupHandler...");
    if (nonNull(DB_POOL)) {
      return Future.failedFuture("Database has already been initialized");
    }
    return Future.future(registerHandler -> {
      PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(CONFIGURATION.port())
        .setHost(CONFIGURATION.host())
        .setDatabase(CONFIGURATION.name())
        .setUser(CONFIGURATION.user())
        .setPassword(CONFIGURATION.password());
      PoolOptions poolOptions = new PoolOptions().setMaxSize(20);
      DB_POOL = PgPool.pool(vertx, connectOptions, poolOptions);
      DB_POOL.getConnection()
        .onSuccess(connection -> {
          LOGGER.info("DatabaseHandler initialized");
          new DatabaseMigrationHandler(CONFIGURATION).execute(vertx, registerHandler);
        })
        .onFailure(error -> {
          LOGGER.error("Error while registering DatabaseHandler", error);
          registerHandler.fail(error);
        });
    });
  }

  @Override
  public void closeResources() {
    LOGGER.info("Closing resources on DatabaseSetupHandler...");
    DB_POOL.close();
  }

  public Future<RowSet<Row>> executeQuery(String query, List<Object> params) {
    return new DatabaseQueryHandler(DB_POOL).execute(query, params);
  }

  public DatabaseTransactionHandler initTransaction() {
    return new DatabaseTransactionHandler(DB_POOL);
  }
}
