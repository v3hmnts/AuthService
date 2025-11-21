package authService.service;

import authService.dto.UserRegistrationRequest;
import authService.entity.Role;
import authService.entity.User;
import authService.repository.RoleRepository;
import authService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final String DEFAULT_USER_ROLE="ROLE_USER";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserRegistrationRequest userRegistrationRequest) {
        if (userRepository.existsByUsername(userRegistrationRequest.username())) {
            throw new RuntimeException("Username is already taken");
        }

        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new RuntimeException("Email is already in use");
        }
        Role defaultRole = roleRepository.findByName(DEFAULT_USER_ROLE).orElseThrow(()->new RuntimeException(String.format("Default role %s not found",DEFAULT_USER_ROLE)));
        User user = new User(userRegistrationRequest.username(), passwordEncoder.encode(userRegistrationRequest.password()),userRegistrationRequest.email());
        user.getRoles().add(defaultRole);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean validateUserCredentials(User user,String username, String password) {
        return user.getUsername().equals(username) && passwordEncoder.matches(password, user.getPassword());

    }
}
