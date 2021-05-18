package tatsumibruno.order.api.query_order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.api.domain.OrderStatus;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinishedOrder {
  private String code;
  private OrderStatus status;
}
