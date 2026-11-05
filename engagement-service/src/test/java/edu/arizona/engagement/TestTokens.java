package edu.arizona.engagement;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import edu.arizona.security.JwtClaims;

/** Signs tokens the same way identity-service does, with the dev profile's secret. */
final class TestTokens {

    private static final String SECRET = "dev-only-secret-do-not-use-in-prod-0123456789abcdef";

    private TestTokens() {
    }

    static String bearer(long userId, String username, String... roles) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(JwtClaims.secretKey(SECRET)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cool-engineering-games")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject(String.valueOf(userId))
                .claim(JwtClaims.USERNAME, username)
                .claim(JwtClaims.ROLES, List.of(roles))
                .build();
        String token = encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return "Bearer " + token;
    }
}
