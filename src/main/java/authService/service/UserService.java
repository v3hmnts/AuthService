package authService.service;

import authService.dto.RegistrationRequest;
import authService.dto.UserRegistrationRequest;
import authService.dto.UserServiceUserDto;
import authService.entity.Role;
import authService.entity.RoleType;
import authService.entity.User;
import authService.exception.RoleNotFoundException;
import authService.exception.UserAlreadyExistsException;
import authService.exception.UserNotFoundException;
import authService.repository.RoleRepository;
import authService.repository.UserRepository;
import authService.security.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final RoleType DEFAULT_USER_ROLE = RoleType.ROLE_USER;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public User createUser(RegistrationRequest userRegistrationRequest, RoleType roleType) {
        if (userRepository.existsByUsername(userRegistrationRequest.username())) {
            throw new UserAlreadyExistsException("User with given username is already taken");
        }

        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new UserAlreadyExistsException("User with given email is already in use");
        }
        Role userRole = roleRepository.findByName(roleType).orElseThrow(() -> new RoleNotFoundException(roleType.toString()));
        User user = new User(userRegistrationRequest.username(), passwordEncoder.encode(userRegistrationRequest.password()), userRegistrationRequest.email());
        user.getRoles().add(userRole);

        String adminToken = jwtUtil.generateAdminAccessToken();
        UserServiceUserDto userDto = new UserServiceUserDto(null,user.getUsername(), userRegistrationRequest.surname(), userRegistrationRequest.birthDate(),user.getEmail(),true);

        UserServiceUserDto userServiceUserDto = RestClient.create("http://172.17.0.1:8080/api/v1/users")
                .post()
                .header("Authorization", "Bearer " + adminToken)
                .body(userDto)
                .retrieve()
                .body(UserServiceUserDto.class);

        if(userServiceUserDto==null){
            throw new RuntimeException("Failed to create user");
        }
        user.setId(userServiceUserDto.id());
        return userRepository.save(user);
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
        return userRepository.findByUsername(username).orElseThrow(()-> new UserNotFoundException(username));
    }
}
