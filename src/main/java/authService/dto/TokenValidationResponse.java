package authService.dto;

public record TokenValidationResponse(
        boolean valid,
        String username,
        String message
) {
}
