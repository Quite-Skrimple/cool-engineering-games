package edu.arizona.engagement.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
public class CatalogClientConfig {

    /**
     * Resolves http://game-catalog-service through Eureka and spreads calls over every running instance.
     *
     * Built here as a finished RestClient on purpose. Declaring a @LoadBalanced RestClient.Builder bean
     * would replace the default builder that the Eureka client itself uses, and Eureka would then try to
     * find the discovery server by asking Eureka.
     */
    @Bean
    public RestClient catalogRestClient(LoadBalancerClient loadBalancerClient,
            @Value("${app.catalog.url}") String catalogUrl) {

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(catalogUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new LoadBalancerInterceptor(
                        loadBalancerClient, new LoadBalancerRequestFactory(loadBalancerClient)))
                .build();
    }

    /**
     * Opens the catalog breaker once half of the last 10 calls failed, so a struggling catalog is left
     * alone for 10 seconds instead of stacking up slow requests, then lets 3 probe calls through.
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> catalogCircuitBreaker() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()),
                GameCatalogClient.BREAKER_NAME);
    }
}
