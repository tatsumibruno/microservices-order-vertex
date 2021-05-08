package tatsumibruno.order.api.commons.handlers.db;

import java.util.List;

record DatabaseTransactionOperation(String query, List<Object> params) {
}
