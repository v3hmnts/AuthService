package authService.service;

import authService.controller.AuthController;
import authService.dto.AuthRequest;
import authService.dto.AuthResponse;
import authService.dto.TokenValidationResponse;
import authService.entity.RefreshToken;
import authService.entity.User;
import authService.repository.RefreshTokenRepository;
import authService.repository.UserRepository;
import authService.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;

    public TokenService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil, UserRepository userRepository, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public TokenValidationResponse validateAuthenticationToken(String token){

        if(jwtUtil.validateToken(token)){
            return new TokenValidationResponse(true,jwtUtil.getUsernameFromToken(token),"Token is valid");
        }else {
            return new TokenValidationResponse(false,null,"Token is not valid");
        }
    }

    public AuthResponse createAuthenticationToken(AuthRequest authRequest){
            User user = userRepository.findByUsername(authRequest.username()).orElseThrow(()->new RuntimeException("User not found"));

            if (!userService.validateUserCredentials(user,authRequest.username(), authRequest.password())) {
                throw new RuntimeException("Invalid credentials");
            }

            final String accessToken = jwtUtil.generateAccessToken(user);
            final String refreshToken = jwtUtil.generateRefreshToken();

            // Create and save refresh token entity
            refreshTokenRepository.save(new RefreshToken(refreshToken,user,LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs()/1000)));

            // Update last login
            userService.updateLastLogin(authRequest.username());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getJwtExpirationMs(),
                jwtUtil.getRefreshExpirationMs()
        );
    }


    public AuthResponse createNewAuthTokenWithRefreshToken(String refreshToken){
        RefreshToken refreshToken1 = refreshTokenRepository.findByToken(refreshToken).orElseThrow(()->new RuntimeException("Refresh token not found"));
        if(refreshToken1.getExpiryDate().isBefore(LocalDateTime.now())){
            refreshTokenRepository.delete(refreshToken1);
            throw new RuntimeException("Refresh token is expired");
        }
        return new AuthResponse(
                jwtUtil.generateAccessToken(refreshToken1.getUser()),
                refreshToken,
                jwtUtil.getJwtExpirationMs(),
                jwtUtil.getRefreshExpirationMs()
        );
    }

    public Optional<RefreshToken> findRefreshTokenByToken(String token) {
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
