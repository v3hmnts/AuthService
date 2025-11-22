package authService.service;

import authService.dto.UserRegistrationRequest;
import authService.entity.Role;
import authService.entity.RoleType;
import authService.entity.User;
import authService.exception.RoleNotFoundException;
import authService.exception.UserAlreadyExistsException;
import authService.exception.UserNotFoundException;
import authService.repository.RoleRepository;
import authService.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final RoleType DEFAULT_USER_ROLE = RoleType.ROLE_USER;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserRegistrationRequest userRegistrationRequest,RoleType roleType) {
        if (userRepository.existsByUsername(userRegistrationRequest.username())) {
            throw new UserAlreadyExistsException("User with given username is already taken");
        }

        if (userRepository.existsByEmail(userRegistrationRequest.email())) {
            throw new UserAlreadyExistsException("User with given email is already in use");
        }
        Role userRole = roleRepository.findByName(roleType).orElseThrow(() -> new RoleNotFoundException(roleType.toString()));
        User user = new User(userRegistrationRequest.username(), passwordEncoder.encode(userRegistrationRequest.password()), userRegistrationRequest.email());
        user.getRoles().add(userRole);
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
