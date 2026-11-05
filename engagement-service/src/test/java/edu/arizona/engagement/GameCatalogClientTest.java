package edu.arizona.engagement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import edu.arizona.engagement.config.GameCatalogClient;
import edu.arizona.engagement.exception.ServiceUnavailableException;

@SuppressWarnings({ "unchecked", "rawtypes" })
class GameCatalogClientTest {

    private MockRestServiceServer server;
    private GameCatalogClient client;
    private boolean breakerOpen;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://game-catalog-service");
        server = MockRestServiceServer.bindTo(builder).build();

        // a breaker that runs the call, or short-circuits straight to the fallback when "open"
        CircuitBreaker breaker = mock(CircuitBreaker.class);
        when(breaker.run(any(Supplier.class), any(Function.class))).thenAnswer(invocation -> {
            Supplier<Object> call = invocation.getArgument(0);
            Function<Throwable, Object> fallback = invocation.getArgument(1);
            if (breakerOpen) {
                return fallback.apply(new IllegalStateException("circuit open"));
            }
            try {
                return call.get();
            } catch (Throwable failure) {
                return fallback.apply(failure);
            }
        });
        CircuitBreakerFactory factory = mock(CircuitBreakerFactory.class);
        when(factory.create(GameCatalogClient.BREAKER_NAME)).thenReturn(breaker);

        client = new GameCatalogClient(builder.build(), factory);

        Jwt jwt = Jwt.withTokenValue("the-callers-token").header("alg", "HS256").subject("11").build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_PLAYER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void forwardsTheCallersOwnBearerTokenToTheCatalog() {
        server.expect(requestTo("http://game-catalog-service/api/games/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer the-callers-token"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.requireVisibleGame(7L)).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void aMissingOrHiddenGameIsReportedAsNotFound() {
        server.expect(requestTo("http://game-catalog-service/api/games/8"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.requireVisibleGame(8L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("8");
    }

    @Test
    void aServerErrorFromTheCatalogBecomesServiceUnavailable() {
        server.expect(requestTo("http://game-catalog-service/api/games/9")).andRespond(withServerError());

        assertThatThrownBy(() -> client.requireVisibleGame(9L)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void anUnauthorizedAnswerIsNotMistakenForAMissingGame() {
        server.expect(requestTo("http://game-catalog-service/api/games/10"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.requireVisibleGame(10L)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void aFailedConnectionIsRetriedOnceAndCanSucceedOnTheNextInstance() {
        server.expect(requestTo("http://game-catalog-service/api/games/13"))
                .andRespond(withException(new java.io.IOException("Connection refused")));
        server.expect(requestTo("http://game-catalog-service/api/games/13"))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.requireVisibleGame(13L)).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void twoFailedConnectionsInARowBecomeServiceUnavailable() {
        server.expect(requestTo("http://game-catalog-service/api/games/14"))
                .andRespond(withException(new java.io.IOException("Connection refused")));
        server.expect(requestTo("http://game-catalog-service/api/games/14"))
                .andRespond(withException(new java.io.IOException("Connection refused")));

        assertThatThrownBy(() -> client.requireVisibleGame(14L)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void anOpenCircuitFailsFastWithoutCallingTheCatalog() {
        breakerOpen = true;

        assertThatThrownBy(() -> client.requireVisibleGame(11L)).isInstanceOf(ServiceUnavailableException.class);

        server.verify(); // no request was expected, so none may have been made
    }

    @Test
    void withoutABearerTokenThereIsNothingToForward() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> client.requireVisibleGame(12L)).isInstanceOf(IllegalStateException.class);
    }
}
