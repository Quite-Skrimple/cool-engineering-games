package edu.arizona.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.arizona.identity.exception.BadRequestException;
import edu.arizona.identity.model.entity.PasswordResetToken;
import edu.arizona.identity.model.entity.User;
import edu.arizona.identity.model.enums.UserStatus;
import edu.arizona.identity.repository.PasswordResetTokenRepository;
import edu.arizona.identity.repository.UserRepository;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Duration tokenTtl;
    private final String frontendUrl;
    private final boolean logLink;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
            BCryptPasswordEncoder passwordEncoder, MailService mailService,
            @Value("${app.password-reset.ttl:PT30M}") Duration tokenTtl,
            @Value("${app.frontend-url:http://localhost:5500}") String frontendUrl,
            @Value("${app.password-reset.log-link:false}") boolean logLink) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.tokenTtl = tokenTtl;
        this.frontendUrl = frontendUrl;
        this.logLink = logLink;
    }

    /**
     * Emails a reset link if an active account has this address. Does nothing otherwise, and the
     * caller cannot tell the two apart. A new request replaces any earlier unused link.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(UserService.normalizeEmail(email))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::issueToken);
    }

    /** Sets a new password if the token is valid, then burns every outstanding token for that user. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken))
                .filter(t -> !t.isExpired())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
    }

    private void issueToken(User user) {
        tokenRepository.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plus(tokenTtl));
        tokenRepository.save(token);

        String link = frontendUrl + "/pages/auth/reset-password.html?token=" + rawToken;
        if (logLink) {
            log.info("[dev] password reset link for {}: {}", user.getUsername(), link);
        }
        mailService.sendPasswordReset(user.getEmail(), user.getUsername(), link, tokenTtl.toMinutes());
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
