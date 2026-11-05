package edu.arizona.engagement.config;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import edu.arizona.engagement.exception.ServiceUnavailableException;
import edu.arizona.security.CurrentUser;

/**
 * Asks game-catalog-service whether the caller may see a game, forwarding the caller's own bearer
 * token so the catalog applies its visibility rules to the same user. The call goes through a
 * circuit breaker: when the catalog is down the request fails fast with 503 instead of hanging.
 */
@Component
public class GameCatalogClient {

    public static final String BREAKER_NAME = "gameCatalog";

    private static final Logger log = LoggerFactory.getLogger(GameCatalogClient.class);

    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public GameCatalogClient(RestClient catalogRestClient, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = catalogRestClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * @throws NoSuchElementException        if the game does not exist or is not visible to the caller
     * @throws ServiceUnavailableException   if the catalog cannot be reached or its breaker is open
     */
    public void requireVisibleGame(Long gameId) {
        // read on the request thread: the breaker may run the call on another one, where there is no security context
        String token = CurrentUser.jwt().map(Jwt::getTokenValue)
                .orElseThrow(() -> new IllegalStateException("No bearer token to forward"));

        boolean visible = circuitBreakerFactory.create(BREAKER_NAME).run(
                () -> fetch(gameId, token),
                failure -> {
                    log.warn("game-catalog-service call failed: {}", failure.toString());
                    throw new ServiceUnavailableException(
                            "game-catalog-service is unavailable, please try again shortly");
                });

        if (!visible) {
            throw new NoSuchElementException("Game not found: " + gameId);
        }
    }

    /**
     * One retry when the connection itself fails, for example because a crashed catalog instance is still
     * listed. The request is a GET, so repeating it is safe, and the load balancer picks the next instance.
     */
    private boolean fetch(Long gameId, String token) {
        try {
            return call(gameId, token);
        } catch (ResourceAccessException firstFailure) {
            return call(gameId, token);
        }
    }

    private boolean call(Long gameId, String token) {
        return restClient.get()
                .uri("/api/games/{id}", gameId)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        return true;
                    }
                    if (status.value() == 404) {
                        return false;
                    }
                    throw new IllegalStateException("game-catalog-service answered " + status.value());
                });
    }
}
