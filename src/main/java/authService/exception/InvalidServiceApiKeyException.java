package authService.exception;

public class InvalidServiceApiKeyException extends RuntimeException {
    public InvalidServiceApiKeyException() {
        super("Invalid service api key");
    }
}
