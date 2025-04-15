package labs.exception;

import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionResponse notFoundExceptionHandler(NotFoundException ex, WebRequest request) {
        return new ExceptionResponse(ex.getMessage(), request.getDescription(false));
    }

    @ResponseBody
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse constraintViolationExceptionHandler(ConstraintViolationException ex) {
        List<ValidationError> errors = ex.getConstraintViolations()
                .stream()
                .map(violation ->
                        new ValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());
        return new ValidationErrorResponse(errors);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse methodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error ->
                        new ValidationError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ValidationErrorResponse(errors);
    }

    @ResponseBody
    @ExceptionHandler(JsonPatchException.class)
    public ResponseEntity<ExceptionResponse> jsonPatchExceptionHandler(JsonPatchException ex,
                WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> methodArgumentTypeMismatchExceptionHandler(
            MethodArgumentTypeMismatchException ex) {
        String message = "Failed to convert value of type '" +
                Objects.requireNonNull(ex.getValue()).getClass().getSimpleName() +
                "' to required type '" + ex.getRequiredType().getSimpleName() + "'";
        ExceptionResponse exceptionResponse = new ExceptionResponse(message, ex.getPropertyName());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> httpMessageNotReadableExceptionHandler(
            HttpMessageNotReadableException ex, WebRequest request) {
        String message = ex.getMessage();
        if (message.contains(": Failed")) {
            message = message.substring(0, message.indexOf(": Failed"));
        }
        ExceptionResponse exceptionResponse = new ExceptionResponse(message, request.getDescription(false));
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ExceptionHandler(MissingPathVariableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse missingPathVariableExceptionHandler(MissingPathVariableException ex,
                WebRequest request) {
        return new ExceptionResponse(ex.getMessage(), request.getDescription(false));
    }

    @ResponseBody
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse validationExceptionHandler(ValidationException ex, WebRequest request) {
        return new ExceptionResponse(ex.getMessage(), request.getDescription(false));
    }
}
