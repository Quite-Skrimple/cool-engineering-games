package edu.arizona.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.identity.dto.RoleUpdateRequest;
import edu.arizona.identity.dto.StatusUpdateRequest;
import edu.arizona.identity.dto.UserRequest;
import edu.arizona.identity.dto.UserResponse;
import edu.arizona.identity.dto.UserUpdateRequest;
import edu.arizona.identity.service.UserService;
import edu.arizona.security.CurrentUser;
import jakarta.validation.Valid;

/**
 * Account management. A user may read, edit and delete their own account;
 * everything else needs the ADMIN role.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String SELF_OR_ADMIN = "hasRole('ADMIN') or #id.toString() == authentication.name";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getUser(CurrentUser.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize(SELF_OR_ADMIN)
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(SELF_OR_ADMIN)
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateProfile(id, request);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return userService.changeRole(id, request.getRole());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return userService.changeStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SELF_OR_ADMIN)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
