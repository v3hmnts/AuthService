package authService.service;

import authService.dto.*;
import authService.entity.RefreshToken;
import authService.entity.User;
import authService.exception.InvalidServiceApiKeyException;
import authService.exception.RefreshTokenExpiredException;
import authService.exception.RefreshTokenNotFoundException;
import authService.exception.UserNotFoundException;
import authService.repository.RefreshTokenRepository;
import authService.repository.UserRepository;
import authService.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public TokenService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public TokenValidationResponse validateAuthenticationToken(String token) throws Exception {
        if (jwtUtil.validateToken(token)) {
            return new TokenValidationResponse(true, jwtUtil.getUsernameFromToken(token), "Token is valid");
        }
        return new TokenValidationResponse(false, null, "Token is not valid");
    }

    public AuthResponse createAuthenticationToken(User user) throws Exception {

        final String accessToken = jwtUtil.generateAccessToken(user);
        final String refreshToken = jwtUtil.generateRefreshToken();

        refreshTokenRepository.save(new RefreshToken(refreshToken, user, LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000)));

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getJwtExpirationMs(),
                jwtUtil.getRefreshExpirationMs()
        );
    }

    public ServiceAuthResponse createNewServiceToken(String apiKey) throws Exception {
            final String accessToken = jwtUtil.generateServiceAccessToken(apiKey);
            return new ServiceAuthResponse(
                    accessToken,
                    jwtUtil.getJwtExpirationMs()
            );
    }


    public AuthResponse createNewAuthTokenWithRefreshToken(String refreshToken) throws Exception {
        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));
        if (savedRefreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(savedRefreshToken);
            throw new RefreshTokenExpiredException("Refresh token is expired");
        }
        return new AuthResponse(
                jwtUtil.generateAccessToken(savedRefreshToken.getUser()),
                refreshToken,
                jwtUtil.getJwtExpirationMs(),
                jwtUtil.getRefreshExpirationMs()
        );
    }

    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        refreshTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtUtil.getJwtExpirationMs() / 1000);

        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens();
    }
}
