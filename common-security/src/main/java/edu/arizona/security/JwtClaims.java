package edu.arizona.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/** Claim names and key handling shared by the token issuer, the services and the gateway. */
public final class JwtClaims {

    public static final String USERNAME = "username";
    public static final String ROLES = "roles";

    private static final int MIN_SECRET_BYTES = 32;

    private JwtClaims() {
    }

    public static SecretKey secretKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.secret is not set. Set the JWT_SECRET environment variable.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
