package edu.arizona.ceg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * SecurityBeans Configuration
 *
 * Defines security-related infrastructure beans.
 * Provides BCryptPasswordEncoder for password hashing
 */
@Configuration
public class SecurityBeans {

    /**
     * BCrypt password encoder bean.
     * Injected into UserService for secure password storage.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}