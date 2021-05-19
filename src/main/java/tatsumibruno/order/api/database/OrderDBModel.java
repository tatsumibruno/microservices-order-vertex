package tatsumibruno.order.api.database;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import tatsumibruno.order.api.domain.OrderCustomer;
import tatsumibruno.order.api.domain.OrderStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(of = "code")
public class OrderDBModel {
    @Setter
    private Long id;
    @Setter
    private ZonedDateTime createdAt;
    @Setter
    private OrderStatus status;
    private final UUID code;
    private final OrderCustomer customer;
}
