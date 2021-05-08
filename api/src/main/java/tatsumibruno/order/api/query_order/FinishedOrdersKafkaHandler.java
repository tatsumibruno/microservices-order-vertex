package tatsumibruno.order.api.query_order;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import tatsumibruno.order.api.commons.handlers.KafkaHandler;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public enum FinishedOrdersKafkaHandler implements KafkaHandler {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(FinishedOrdersKafkaHandler.class);

  @Override
  public void register(Vertx vertx) {
    LOGGER.info("Registering FinishedOrdersKafkaHandler...");
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", "order-api");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");

    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);
    consumer.handler(record -> {
      LOGGER.info(format("Recebendo registro com chave %s do tópico %s ", record.key(), record.topic()));
      LOGGER.info(record.value());
    });
    consumer.subscribe("finished-orders");
  }
}
