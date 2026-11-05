package edu.arizona.identity.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.identity.dto.RegisterRequest;
import edu.arizona.identity.dto.UserRequest;
import edu.arizona.identity.dto.UserResponse;
import edu.arizona.identity.dto.UserUpdateRequest;
import edu.arizona.identity.exception.ConflictException;
import edu.arizona.identity.model.entity.User;
import edu.arizona.identity.model.enums.UserRole;
import edu.arizona.identity.model.enums.UserStatus;
import edu.arizona.identity.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Public sign-up. Always creates a PLAYER: elevated roles are granted by an administrator. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        return toResponse(create(request.getUsername(), request.getEmail(), request.getPassword(), UserRole.PLAYER));
    }

    /** Administrator-created account with an explicit role. */
    @Transactional
    public UserResponse createUser(UserRequest request) {
        return toResponse(create(request.getUsername(), request.getEmail(), request.getPassword(), request.getRole()));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return toResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateProfile(Long id, UserUpdateRequest request) {
        User user = findUserOrThrow(id);
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already in use: " + username);
        }
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use: " + email);
        }

        user.setUsername(username);
        user.setEmail(email);
        return toResponse(userRepository.save(user));
    }

    /** Promoting a PLAYER to DEVELOPER is how a creator gets verified. */
    @Transactional
    public UserResponse changeRole(Long id, UserRole role) {
        User user = findUserOrThrow(id);
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN) {
            requireAnotherAdmin();
        }
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse changeStatus(Long id, UserStatus status) {
        User user = findUserOrThrow(id);
        if (user.getRole() == UserRole.ADMIN && status == UserStatus.SUSPENDED) {
            requireAnotherAdmin();
        }
        user.setStatus(status);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        if (user.getRole() == UserRole.ADMIN) {
            requireAnotherAdmin();
        }
        userRepository.delete(user);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private User create(String rawUsername, String rawEmail, String rawPassword, UserRole role) {
        String username = rawUsername.trim();
        String email = normalizeEmail(rawEmail);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already in use: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void requireAnotherAdmin() {
        if (userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ConflictException("The last administrator cannot be removed, demoted or suspended");
        }
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }
}
