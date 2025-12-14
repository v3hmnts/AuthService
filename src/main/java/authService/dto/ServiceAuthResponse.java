package authService.dto;

public record ServiceAuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn
) {
    public ServiceAuthResponse(String accessToken, Long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}
