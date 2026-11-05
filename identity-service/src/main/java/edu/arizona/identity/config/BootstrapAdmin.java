package edu.arizona.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import edu.arizona.identity.dto.UserRequest;
import edu.arizona.identity.model.enums.UserRole;
import edu.arizona.identity.repository.UserRepository;
import edu.arizona.identity.service.UserService;

/**
 * Creates the first administrator at startup, because there is no way to become one through the API.
 * Does nothing unless a username and password are configured, or if that username already exists.
 */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdmin(UserService userService, UserRepository userRepository,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userService.createUser(new UserRequest(username, email, password, UserRole.ADMIN));
        log.info("Created bootstrap administrator '{}'", username);
    }
}
