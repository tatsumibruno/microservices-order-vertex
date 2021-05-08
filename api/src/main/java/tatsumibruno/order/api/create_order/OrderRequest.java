package tatsumibruno.order.api.create_order;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class OrderRequest {
  private String customerName;
  private String customerEmail;
  private String deliveryAddress;
}
