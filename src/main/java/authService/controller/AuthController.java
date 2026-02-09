package authService.controller;

import authService.dto.*;
import authService.entity.RoleType;
import authService.entity.User;
import authService.service.TokenService;
import authService.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, TokenService tokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        userService.createUser(registrationRequest, RoleType.ROLE_USER);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/register/admin")
    public ResponseEntity<Void> registerAdmin(@Valid @RequestBody RegistrationRequest registrationRequest) {
        userService.createUser(registrationRequest,RoleType.ROLE_ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) throws Exception {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.username(),authRequest.password()));
        User user = userService.loadUserByUsername(authRequest.username());
        AuthResponse authResponse = tokenService.createAuthenticationToken(user);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest tokenValidationRequest) throws Exception {
        return ResponseEntity.ok(tokenService.validateAuthenticationToken(tokenValidationRequest.token()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception {
        return ResponseEntity.ok(tokenService.createNewAuthTokenWithRefreshToken(refreshTokenRequest.refreshToken()));
    }

}
