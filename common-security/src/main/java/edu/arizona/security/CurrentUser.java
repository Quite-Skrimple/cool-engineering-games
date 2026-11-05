package edu.arizona.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The caller of the current request, read from the verified JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<Jwt> jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.of(token.getToken());
        }
        return Optional.empty();
    }

    public static Long id() {
        Jwt jwt = jwt().orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return Long.valueOf(jwt.getSubject());
    }

    public static String username() {
        return jwt().orElseThrow(() -> new IllegalStateException("No authenticated user"))
                .getClaimAsString(JwtClaims.USERNAME);
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /** True when the caller owns the resource or is an administrator. */
    public static boolean isOwnerOrAdmin(Long ownerId) {
        return isAdmin() || id().equals(ownerId);
    }
}
