package tatsumibruno.order.api.query_order;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import tatsumibruno.order.api.commons.handlers.DatabaseHandler;
import tatsumibruno.order.api.commons.handlers.KafkaHandler;
import tatsumibruno.order.api.commons.infra.KafkaUtils;

import java.util.List;

import static java.lang.String.format;

public enum OrdersStatusChangesKafkaHandler implements KafkaHandler {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(OrdersStatusChangesKafkaHandler.class);

  @Override
  public void register(Vertx vertx) {
    LOGGER.info("Registering FinishedOrdersKafkaHandler...");
    KafkaConsumer<String, String> finishedOrdersConsumer = KafkaUtils.consumer(vertx, "order-api");
    KafkaProducer<String, String> finishedOrdersDlxProducer = KafkaUtils.producer(vertx);
    finishedOrdersConsumer.handler(record -> {
      LOGGER.info(format("Receiving register with key %s on topic %s ", record.key(), record.topic()));
      FinishedOrder finishedOrder = Json.decodeValue(record.value(), FinishedOrder.class);
      String code = finishedOrder.getCode();
      DatabaseHandler.INSTANCE
        .executeQuery("SELECT 1 FROM ORDERS WHERE CODE = $1", List.of(code))
        .onSuccess(result -> {
          if (result.rowCount() == 0) {
            LOGGER.warn("Order " + code + " not found in database");
            finishedOrdersDlxProducer.send(KafkaProducerRecord.create(
              "orders-changes-dlx",
              code,
              Json.encode(finishedOrder)));
            finishedOrdersConsumer.commit();
          } else {
            DatabaseHandler.INSTANCE.initTransaction()
              .addOperation("UPDATE ORDERS SET STATUS = $1 WHERE CODE = $2", List.of(finishedOrder.getStatus(), code))
              .execute()
              .onSuccess(unused -> {
                LOGGER.info("Order " + code + " finished with status " + finishedOrder.getStatus());
                finishedOrdersConsumer.commit();
              })
              .onFailure(failure -> LOGGER.error("Error while updating order " + code, failure));
          }
        })
        .onFailure(failure -> LOGGER.error("Error while searching for order " + code, failure));
    });
    finishedOrdersConsumer.subscribe("orders-changes");
  }
}
