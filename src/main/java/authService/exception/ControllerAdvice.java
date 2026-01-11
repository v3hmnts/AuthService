package authService.exception;

import authService.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                errors
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleUserRegistrationException(UserRegistrationException exception) {
        return new ResponseEntity<>(new ErrorResponse(Instant.now(), exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(RuntimeException exception) {
        return new ResponseEntity<>(new ErrorResponse(Instant.now(), exception.getMessage(), HttpStatus.CONFLICT, null), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({RefreshTokenExpiredException.class, RefreshTokenNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleERefreshTokenException(RuntimeException exception) {
        return new ResponseEntity<>(new ErrorResponse(Instant.now(), exception.getMessage(), HttpStatus.UNAUTHORIZED, null), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({RoleNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException exception) {
        return new ResponseEntity<>(new ErrorResponse(Instant.now(), exception.getMessage(), HttpStatus.NOT_FOUND, null), HttpStatus.NOT_FOUND);
    }
}
