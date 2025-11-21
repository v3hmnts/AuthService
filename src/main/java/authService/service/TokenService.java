package authService.service;

import authService.controller.AuthController;
import authService.dto.AuthRequest;
import authService.dto.AuthResponse;
import authService.entity.RefreshToken;
import authService.entity.User;
import authService.repository.RefreshTokenRepository;
import authService.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    public String createAuthenticationToken(AuthRequest authRequest){
        try {
            if (!userService.validateUserCredentials(authRequest.username(), authRequest.password())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthController.ErrorResponse("Invalid credentials"));
            }

            final String accessToken = jwtUtil.generateAccessToken(authRequest.username());
            final String refreshToken = jwtUtil.generateRefreshToken(authRequest.password());

            // Create and save refresh token entity
            tokenService.createRefreshToken(authRequest.username());

            // Update last login
            userService.updateLastLogin(authRequest.username());

            AuthResponse authResponse = new AuthResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getJwtExpirationMs()
            );

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthController.ErrorResponse("Error creating token: " + e.getMessage()));
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));


        refreshTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtUtil.getJwtExpirationMs() / 1000);

        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
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
