package tatsumibruno.order.api.commons;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

  private String code;
  private String message;

  public static ErrorResponse of(@NonNull Throwable failure, @NonNull String message) {
    return new ErrorResponse(failure.getClass().getSimpleName(), message);
  }

  public static ErrorResponse of(@NonNull String code, @NonNull String message) {
    return new ErrorResponse(code, message);
  }
}
