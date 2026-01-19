package authService.controller;

import authService.dto.*;
import authService.entity.RoleType;
import authService.entity.User;
import authService.security.SecurityUser;
import authService.service.TokenService;
import authService.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, TokenService tokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) throws Exception {
        logger.info("POST request to /register endpoint received");
        userService.createUser(registrationRequest, RoleType.ROLE_USER);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/token")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) throws Exception {
        logger.info("POST request to /token endpoint received");
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));
        SecurityUser securityUser = userService.loadUserByUsername(authRequest.username());
        AuthResponse authResponse = tokenService.createAuthenticationToken(securityUser);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest tokenValidationRequest) throws Exception {
        logger.info("POST request to /validate endpoint received");
        return ResponseEntity.ok(tokenService.validateAuthenticationToken(tokenValidationRequest.token()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception {
        logger.info("POST request to /refresh endpoint received");
        return ResponseEntity.ok(tokenService.createNewAuthTokenWithRefreshToken(refreshTokenRequest.refreshToken()));
    }

}
