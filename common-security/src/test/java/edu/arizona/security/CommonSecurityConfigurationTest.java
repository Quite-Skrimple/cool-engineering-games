package edu.arizona.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

class CommonSecurityConfigurationTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private static final String ISSUER = "cool-engineering-games";

    private final CommonSecurityConfiguration configuration = new CommonSecurityConfiguration();

    private SecurityProperties properties(String secret) {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret(secret);
        return properties;
    }

    private String token(String secret, String issuer, Instant expiresAt) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(JwtClaims.secretKey(secret)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .subject("42")
                .claim(JwtClaims.USERNAME, "wilbur")
                .claim(JwtClaims.ROLES, List.of("PLAYER", "DEVELOPER"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void decoderAcceptsTokenSignedWithTheSharedSecret() {
        JwtDecoder decoder = configuration.jwtDecoder(properties(SECRET));

        Jwt jwt = decoder.decode(token(SECRET, ISSUER, Instant.now().plusSeconds(600)));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(JwtClaims.USERNAME)).isEqualTo("wilbur");
    }

    @Test
    void decoderRejectsTokenSignedWithAnotherSecret() {
        JwtDecoder decoder = configuration.jwtDecoder(properties(SECRET));
        String forged = token("another-secret-another-secret-another-0000", ISSUER, Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void decoderRejectsExpiredToken() {
        JwtDecoder decoder = configuration.jwtDecoder(properties(SECRET));
        String expired = token(SECRET, ISSUER, Instant.now().minusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void decoderRejectsWrongIssuer() {
        JwtDecoder decoder = configuration.jwtDecoder(properties(SECRET));
        String other = token(SECRET, "someone-else", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(other)).isInstanceOf(JwtException.class);
    }

    @Test
    void rolesClaimBecomesRolePrefixedAuthorities() {
        JwtDecoder decoder = configuration.jwtDecoder(properties(SECRET));
        Jwt jwt = decoder.decode(token(SECRET, ISSUER, Instant.now().plusSeconds(600)));

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) configuration.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_PLAYER", "ROLE_DEVELOPER")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    void missingOrShortSecretFailsFast() {
        assertThatThrownBy(() -> configuration.jwtDecoder(properties(null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> configuration.jwtDecoder(properties("too-short")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }
}
