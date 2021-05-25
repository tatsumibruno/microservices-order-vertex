package tatsumibruno.order.command;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import tatsumibruno.order.commons.handlers.KafkaHandler;
import tatsumibruno.order.commons.infra.KafkaUtils;
import tatsumibruno.order.domain.OrderStatusChange;
import tatsumibruno.order.infra.OrderRepository;

import java.util.UUID;

import static java.lang.String.format;

public enum OrdersStatusChangesKafkaHandler implements KafkaHandler {

  INSTANCE;

  private static final Logger LOGGER = LoggerFactory.getLogger(OrdersStatusChangesKafkaHandler.class);

  @Override
  public void register(Vertx vertx) {
    LOGGER.info("Registering OrdersStatusChangesKafkaHandler...");
    KafkaConsumer<String, String> orderUpdatesConsumer = KafkaUtils.consumer(vertx, "order-api");
    KafkaProducer<String, String> orderUpdatesDlxProducer = KafkaUtils.producer(vertx);
    orderUpdatesConsumer.handler(kafkaRecord -> {
      LOGGER.info(format("Receiving record with key %s on topic %s ", kafkaRecord.key(), kafkaRecord.topic()));
      OrderStatusChange orderStatusChange = Json.decodeValue(kafkaRecord.value(), OrderStatusChange.class);
      String code = orderStatusChange.getCode();
      try {
        OrderRepository.INSTANCE.findByCode(UUID.fromString(code))
            .onSuccess(order -> OrderRepository.INSTANCE
                .updateStatus(order.getId(), orderStatusChange.getStatus())
                .onSuccess(unused -> {
                  LOGGER.info("Order " + code + " updated with status " + orderStatusChange.getStatus());
                  orderUpdatesConsumer.commit();
                })
                .onFailure(failure -> LOGGER.error("Error while updating order " + code, failure)))
            .onFailure(failure -> {
              LOGGER.error("Error while searching for order " + code, failure);
              orderUpdatesDlxProducer.send(KafkaProducerRecord.create(
                  "orders-changes-dlx",
                  code,
                  Json.encode(orderStatusChange)));
              orderUpdatesConsumer.commit();
            });
      } catch (Exception e) {
        LOGGER.error("Unexpected error occourred while process order " + code, e);
        orderUpdatesConsumer.commit();
      }
    });
    orderUpdatesConsumer.subscribe("orders-changes");
  }
}
