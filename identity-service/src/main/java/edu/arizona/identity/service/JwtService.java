package edu.arizona.identity.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import edu.arizona.identity.model.entity.User;
import edu.arizona.security.JwtClaims;
import edu.arizona.security.SecurityProperties;

@Service
public class JwtService {

    public record IssuedToken(String value, long expiresInSeconds) {
    }

    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    public JwtService(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** The role is read when the token is issued, so a role change applies at the user's next login. */
    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Duration ttl = properties.getJwt().getTtl();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(String.valueOf(user.getId()))
                .claim(JwtClaims.USERNAME, user.getUsername())
                .claim(JwtClaims.ROLES, List.of(user.getRole().name()))
                .build();

        String token = encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedToken(token, ttl.toSeconds());
    }
}
