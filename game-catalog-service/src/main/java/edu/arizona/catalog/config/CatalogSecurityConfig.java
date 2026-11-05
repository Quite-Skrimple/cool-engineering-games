package edu.arizona.catalog.config;

import org.springframework.context.annotation.Configuration;

import edu.arizona.security.EnableCommonSecurity;

/**
 * Kept off the main class on purpose: test slices such as @DataJpaTest load the main class
 * but skip @Configuration classes, so repository tests do not pull in the web security setup.
 */
@Configuration
@EnableCommonSecurity
public class CatalogSecurityConfig {
}
