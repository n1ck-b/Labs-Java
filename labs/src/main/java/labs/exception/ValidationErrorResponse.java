package labs.exception;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ValidationErrorResponse {
    private final List<ValidationError> errors;
}
