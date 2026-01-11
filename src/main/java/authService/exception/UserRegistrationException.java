package authService.exception;


import authService.dto.ErrorResponse;
import lombok.Getter;

public class UserRegistrationException extends RuntimeException {
    @Getter
    private final String errorResponse;

    public UserRegistrationException(String errorResponse) {
        this.errorResponse = errorResponse;
    }
}
