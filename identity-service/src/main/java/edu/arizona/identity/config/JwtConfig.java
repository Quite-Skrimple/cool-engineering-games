package edu.arizona.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import edu.arizona.security.JwtClaims;
import edu.arizona.security.SecurityProperties;

/** identity-service signs tokens with the same secret every service uses to verify them. */
@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(JwtClaims.secretKey(properties.getJwt().getSecret())));
    }
}
