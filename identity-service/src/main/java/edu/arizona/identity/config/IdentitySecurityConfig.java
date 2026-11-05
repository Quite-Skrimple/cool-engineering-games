package edu.arizona.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import edu.arizona.security.EnableCommonSecurity;

/**
 * Kept off the main class on purpose: test slices such as @DataJpaTest load the main class
 * but skip @Configuration classes, so repository tests do not pull in the web security setup.
 */
@Configuration
@EnableCommonSecurity
@EnableAsync
public class IdentitySecurityConfig {
}
