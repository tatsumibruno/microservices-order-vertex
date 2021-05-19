package tatsumibruno.order.api.query_order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.api.commons.Constants;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@ToString
@AllArgsConstructor
public class OrderResponse {
  private UUID code;
  @JsonFormat(pattern = Constants.ISO_DATE_TIME)
  private ZonedDateTime createdAt;
  private String customerName;
  private String customerEmail;
  private String deliveryAddress;
}
