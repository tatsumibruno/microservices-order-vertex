package tatsumibruno.order.api.database;

import tatsumibruno.order.api.domain.OrderCustomer;

import java.util.UUID;

public record OrderDBModel(UUID code, OrderCustomer customer) {
}
