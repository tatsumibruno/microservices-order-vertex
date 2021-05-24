package tatsumibruno.order.api.command;

import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.api.domain.OrderCustomer;

@Getter
@ToString
public class OrderRequest {
  private String customerName;
  private String customerEmail;
  private String deliveryAddress;

  public OrderCustomer toOrderCustomer() {
    return new OrderCustomer(deliveryAddress, customerEmail, deliveryAddress);
  }
}
