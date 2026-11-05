package edu.arizona.gateway;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Opens a route's breaker once half of the last 20 calls failed, then probes again after 10 seconds.
     * A call that takes longer than 8 seconds counts as a failure.
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreaker() {
        return factory -> {
            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                            .slidingWindowSize(20)
                            .minimumNumberOfCalls(10)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(10))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .automaticTransitionFromOpenToHalfOpenEnabled(true)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(8))
                            .build())
                    .build());

            // an operator needs to see a breaker open and close; failure causes are debug-level because they are frequent
            factory.getCircuitBreakerRegistry().getEventPublisher().onEntryAdded(added ->
                    added.getAddedEntry().getEventPublisher()
                            .onStateTransition(event -> log.warn("circuit breaker '{}': {}",
                                    event.getCircuitBreakerName(), event.getStateTransition()))
                            .onError(event -> log.debug("circuit breaker '{}' recorded a failure after {} ms: {}",
                                    event.getCircuitBreakerName(), event.getElapsedDuration().toMillis(),
                                    String.valueOf(event.getThrowable()))));
        };
    }
}
