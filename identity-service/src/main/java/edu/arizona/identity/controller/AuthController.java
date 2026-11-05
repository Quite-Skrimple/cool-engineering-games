package edu.arizona.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.arizona.identity.dto.ForgotPasswordRequest;
import edu.arizona.identity.dto.LoginRequest;
import edu.arizona.identity.dto.MessageResponse;
import edu.arizona.identity.dto.RegisterRequest;
import edu.arizona.identity.dto.ResetPasswordRequest;
import edu.arizona.identity.dto.TokenResponse;
import edu.arizona.identity.dto.UserResponse;
import edu.arizona.identity.service.AuthService;
import edu.arizona.identity.service.PasswordResetService;
import edu.arizona.identity.service.UserService;
import jakarta.validation.Valid;

/** Public endpoints: everything here works without a token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, AuthService authService,
            PasswordResetService passwordResetService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Always answers 202 with the same message, whether or not the address belongs to an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new MessageResponse("If an account exists for that email, a reset link is on its way"));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return new MessageResponse("Password updated. You can now log in with the new password");
    }
}
