package edu.arizona.identity.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.identity.dto.LoginRequest;
import edu.arizona.identity.dto.TokenResponse;
import edu.arizona.identity.exception.ForbiddenException;
import edu.arizona.identity.exception.UnauthorizedException;
import edu.arizona.identity.model.entity.User;
import edu.arizona.identity.model.enums.UserStatus;
import edu.arizona.identity.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    /** Checked when the username is unknown, so a miss takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(UserService.normalizeEmail(identifier)))
                .orElse(null);

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(), user != null ? user.getPasswordHash() : dummyHash);

        if (user == null || !passwordMatches) {
            throw new UnauthorizedException("Invalid username or password");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("This account is suspended");
        }

        JwtService.IssuedToken token = jwtService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds(), userService.toResponse(user));
    }
}
