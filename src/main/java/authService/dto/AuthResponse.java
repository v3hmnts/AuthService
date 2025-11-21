package authService.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long refreshExpiresIn
) {
    public AuthResponse(String accessToken, String refreshToken, Long expiresIn, Long refreshExpiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn, refreshExpiresIn);
    }
}
