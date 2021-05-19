package tatsumibruno.order.api.commons.handlers;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.flywaydb.core.Flyway;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class DatabaseMigrationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationHandler.class);

  private final DatabaseConfiguration configuration;

  void execute(Vertx vertx, Promise<Void> registerHandler) {
    vertx.executeBlocking(migrationHandler -> {
      String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", configuration.host(), configuration.port(), configuration.name());
      Flyway flyway = Flyway.configure()
        .dataSource(jdbcUrl, configuration.user(), configuration.password())
        .baselineOnMigrate(true)
        .load();
      flyway.migrate();
    })
      .onFailure(error -> {
        LOGGER.error("Error on migrate database", error);
        registerHandler.fail(error);
      })
      .onSuccess(unused -> registerHandler.complete());
  }
}
