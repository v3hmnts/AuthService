package authService.service;

import authService.dto.UserRegistrationRequestDto;
import authService.dto.UserServiceUserRegistrationRequestDto;
import authService.dto.UserServiceUserRegistrationResponseDto;
import authService.entity.Role;
import authService.entity.RoleType;
import authService.entity.User;
import authService.exception.RoleNotFoundException;
import authService.exception.UserAlreadyExistsException;
import authService.exception.UserNotFoundException;
import authService.repository.RoleRepository;
import authService.repository.UserRepository;
import authService.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final RoleType DEFAULT_USER_ROLE = RoleType.ROLE_USER;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceRestClient userServiceClient;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserServiceRestClient userServiceClient, TokenService tokenService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.tokenService = tokenService;
        this.jwtUtil = jwtUtil;
    }

    public User createUser(UserRegistrationRequestDto userRegistrationRequest, RoleType roleType) throws Exception {
        if (userRepository.existsByUsername(userRegistrationRequest.username())) {
            throw new UserAlreadyExistsException("User with given username is already taken");
        }

        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new UserAlreadyExistsException("User with given email is already in use");
        }
        Role userRole = roleRepository.findByName(roleType).orElseThrow(() -> new RoleNotFoundException(roleType.toString()));
        User user = new User(userRegistrationRequest.username(), passwordEncoder.encode(userRegistrationRequest.password()), userRegistrationRequest.email());
        user.getRoles().add(userRole);

        UserServiceUserRegistrationRequestDto userRegistrationRequestDto = new UserServiceUserRegistrationRequestDto(
                userRegistrationRequest.name(),
                userRegistrationRequest.surname(),
                userRegistrationRequest.birthDate(),
                userRegistrationRequest.email()
        );
        UserServiceUserRegistrationResponseDto userRegistrationResponseDto = userServiceClient.processUserRegistrationInUserService(userRegistrationRequestDto);
        if (userRegistrationResponseDto.getId() != null) {
            user.setId(userRegistrationResponseDto.getId());
        }
        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            log.error("Compensating userRegistration with userId:{}", userRegistrationResponseDto.getId());
            userServiceClient.compensateUserServiceRegistrationIfFailed(userRegistrationResponseDto.getId());
            throw new RuntimeException("Internal error");
        }
        return user;
    }


    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean validateUserCredentials(User user, String username, String password) {
        return user.getUsername().equals(username) && passwordEncoder.matches(password, user.getPassword());

    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }
}
