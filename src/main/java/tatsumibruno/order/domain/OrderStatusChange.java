package tatsumibruno.order.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusChange {
  private String code;
  private OrderStatus status;
}
