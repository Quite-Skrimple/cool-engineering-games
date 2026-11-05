package edu.arizona.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();

    /** Ant-style paths that skip authentication, on top of the built-in health/error/h2-console ones. */
    private List<String> publicPaths = new ArrayList<>();

    public Jwt getJwt() {
        return jwt;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public static class Jwt {

        /** HMAC-SHA256 signing secret, at least 32 bytes. Shared by every service that issues or verifies tokens. */
        private String secret;

        private String issuer = "cool-engineering-games";

        private Duration ttl = Duration.ofHours(1);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
