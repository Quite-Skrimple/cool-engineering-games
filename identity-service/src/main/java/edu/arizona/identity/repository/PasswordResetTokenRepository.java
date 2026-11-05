package edu.arizona.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.arizona.identity.model.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(Long userId);
}
