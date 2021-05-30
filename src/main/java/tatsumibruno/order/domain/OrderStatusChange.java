package tatsumibruno.order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.ToString;
import tatsumibruno.order.commons.Constants;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Getter
@ToString
public class OrderStatusChange {
    private String code;
    private OrderStatus status;
    @JsonFormat(pattern = Constants.ISO_DATE_TIME)
    private ZonedDateTime timestamp;
}
