package authService.controller;

import authService.dto.AuthRequest;
import authService.dto.AuthResponse;
import authService.dto.TokenValidationResponse;
import authService.dto.UserRegistrationRequest;
import authService.entity.RefreshToken;
import authService.entity.User;
import authService.security.JwtUtil;
import authService.service.TokenService;
import authService.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        try {
            User user = userService.createUser(
                    registrationRequest.username(),
                    registrationRequest.email(),
                    registrationRequest.password()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
        try {
            if (!userService.validateUserCredentials(authRequest.username(), authRequest.password())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid credentials"));
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
                    .body(new ErrorResponse("Error creating token: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                return ResponseEntity.ok(new TokenValidationResponse(true, username, "Token is valid"));
            } else {
                return ResponseEntity.ok(new TokenValidationResponse(false, null, "Token is invalid"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new TokenValidationResponse(false, null, "Token validation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            if (!jwtUtil.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid refresh token"));
            }

            String username = jwtUtil.getUsernameFromToken(refreshToken);

            // Verify refresh token exists in database
            Optional<RefreshToken> storedToken = tokenService.findByToken(refreshToken);
            if (storedToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Refresh token not found"));
            }

            tokenService.verifyExpiration(storedToken.get());

            final String newAccessToken = jwtUtil.generateAccessToken(username);
            final String newRefreshToken = jwtUtil.generateRefreshToken(username);

            // Update refresh token in database
            tokenService.createRefreshToken(username);

            AuthResponse authResponse = new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtUtil.getJwtExpirationMs()
            );

            return ResponseEntity.ok(authResponse);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Error response DTO
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
