package Java.Interview.Assistant.service;
import Java.Interview.Assistant.model.User;
import Java.Interview.Assistant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public String register(
            String name,
            String email,
            String password) {
        email = email.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return "EMAIL_EXISTS";
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(password)
        );
        userRepository.save(user);
        return "SUCCESS";
    }
}