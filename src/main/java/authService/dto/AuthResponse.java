package authService.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn
) {
    public AuthResponse(String accessToken,String refreshToken,Long expiresIn){
        this(accessToken,refreshToken,"Bearer",expiresIn);
    }
}
