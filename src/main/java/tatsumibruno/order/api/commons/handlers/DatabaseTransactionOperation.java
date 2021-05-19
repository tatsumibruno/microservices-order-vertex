package tatsumibruno.order.api.commons.handlers;

import java.util.List;

record DatabaseTransactionOperation(String query, List<Object> params) {
}
