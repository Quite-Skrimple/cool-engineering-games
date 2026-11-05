package edu.arizona.gateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * The gateway must stop unauthenticated and unauthorized requests itself, before any route is
 * tried, so none of these tests needs a running downstream service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.security.jwt.secret=test-secret-test-secret-test-secret-123456",
        "app.cors.allowed-origins=http://localhost:5500"
})
class GatewaySecurityTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @Autowired
    private WebTestClient client;

    private static String bearer(String issuer, Instant expiresAt, String... roles) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(expiresAt.minusSeconds(600))
                .expiresAt(expiresAt)
                .subject("11")
                .claim("username", "alice")
                .claim("roles", List.of(roles))
                .build();
        return "Bearer " + encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String player() {
        return bearer("cool-engineering-games", Instant.now().plusSeconds(600), "PLAYER");
    }

    @Test
    void protectedRoutesRejectRequestsWithoutAToken() {
        client.get().uri("/api/games").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/reactions?gameId=1").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/users/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void forgedExpiredAndForeignTokensAreRejected() {
        client.get().uri("/api/games").header("Authorization", "Bearer not.a.token")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/games")
                .header("Authorization", bearer("cool-engineering-games", Instant.now().minusSeconds(60), "PLAYER"))
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/games")
                .header("Authorization", bearer("someone-else", Instant.now().plusSeconds(600), "PLAYER"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void adminOnlyRoutesAreForbiddenToPlayersBeforeTheyReachAService() {
        client.get().uri("/api/users").header("Authorization", player()).exchange().expectStatus().isForbidden();
        client.put().uri("/api/users/5/role").header("Authorization", player())
                .exchange().expectStatus().isForbidden();
        client.put().uri("/api/games/5/approval").header("Authorization", player())
                .exchange().expectStatus().isForbidden();
        client.post().uri("/api/games").header("Authorization", player())
                .exchange().expectStatus().isForbidden();
        client.get().uri("/actuator/gateway/routes").header("Authorization", player())
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void authEndpointsAndHealthNeedNoToken() {
        // there is no identity-service behind the route in this test, so anything but 401/403 proves it was let through
        client.post().uri("/api/auth/login").exchange()
                .expectStatus().value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403));
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void aBrowserPreflightFromTheFrontendIsAnsweredWithoutAToken() {
        client.options().uri("/api/games")
                .header("Origin", "http://localhost:5500")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5500");
    }

    @Test
    void aForeignOriginIsNotAllowed() {
        client.options().uri("/api/games")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}
