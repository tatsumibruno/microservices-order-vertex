package tatsumibruno.order.api.create_order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.api.commons.Constants;
import tatsumibruno.order.api.commons.handlers.db.DatabaseHandler;
import tatsumibruno.order.api.commons.handlers.db.DatabaseTransactionHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
public class CreatedOrder {

  private static final String SQL_INSERT_ORDER = """
    INSERT INTO orders
    (id, code, created_at, customer_name, customer_email, delivery_address)
    VALUES(nextval('orders_seq'), $1, $2, $3, $4, $5)
    """;

  @JsonIgnore
  private Long id;
  private UUID code;
  @JsonFormat(pattern = Constants.ISO_DATE_TIME)
  private ZonedDateTime createdAt;
  private String customerName;
  private String customerEmail;
  private String deliveryAddress;

  public CreatedOrder(UUID code, ZonedDateTime createdAt, String customerName, String customerEmail, String deliveryAddress) {
    this.code = code;
    this.createdAt = createdAt;
    this.customerName = customerName;
    this.customerEmail = customerEmail;
    this.deliveryAddress = deliveryAddress;
  }

  public Future<Void> save() {
    DatabaseTransactionHandler transaction = DatabaseHandler.INSTANCE.initTransaction();
    transaction.addOperation(SQL_INSERT_ORDER, List.of(code, createdAt.toLocalDateTime(), customerName, customerEmail, deliveryAddress));
    return transaction.execute();
  }
}
