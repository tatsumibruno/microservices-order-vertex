package tatsumibruno.order.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.commons.Constants;
import tatsumibruno.order.domain.Order;
import tatsumibruno.order.domain.OrderCustomer;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@ToString
public class CreatedOrder {

  private UUID code;
  @JsonFormat(pattern = Constants.ISO_DATE_TIME)
  private ZonedDateTime createdAt;
  @JsonIgnore
  private OrderCustomer customer;

  public CreatedOrder(UUID code, ZonedDateTime createdAt, OrderCustomer customer) {
    this.code = code;
    this.createdAt = createdAt;
    this.customer = customer;
  }

  public String getCustomerName() {
    return customer.name();
  }

  public String getCustomerEmail() {
    return customer.email();
  }

  public String getDeliveryAddress() {
    return customer.deliveryAddress();
  }

  public Order toModel() {
    return new Order(code, customer);
  }
}
