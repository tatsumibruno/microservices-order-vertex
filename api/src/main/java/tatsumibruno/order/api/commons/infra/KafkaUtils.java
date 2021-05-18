package tatsumibruno.order.api.commons.infra;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaUtils {

  public static KafkaConsumer<String, String> consumer(Vertx vertx, String groupId) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", groupId);
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");
    return KafkaConsumer.create(vertx, config);
  }

  public static KafkaProducer<String, String> producer(Vertx vertx) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");
    return KafkaProducer.create(vertx, config);
  }
}
