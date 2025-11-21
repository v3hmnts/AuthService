package authService.controller;

import authService.dto.*;
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
            userService.createUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
            AuthResponse authResponse = tokenService.createAuthenticationToken(authRequest);
            return ResponseEntity.ok(authResponse);


    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest tokenValidationRequest) {
        try {
           return ResponseEntity.ok(tokenService.validateAuthenticationToken(tokenValidationRequest.token()));
        } catch (Exception e) {
            return ResponseEntity.ok(new TokenValidationResponse(false, null, "Token validation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(tokenService.createNewAuthTokenWithRefreshToken(refreshTokenRequest.refreshToken()));
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
