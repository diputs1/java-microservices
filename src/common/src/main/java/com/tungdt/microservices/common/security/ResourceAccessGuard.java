package com.tungdt.microservices.common.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ResourceAccessGuard {
    public boolean canAccessOwner(String ownerId) {
        Optional<Authentication> authentication = currentAuthentication();
        if (authentication.isEmpty()) {
            return true;
        }
        return hasRole(authentication.get(), "ADMIN")
                || hasAnyServiceAuthority(authentication.get())
                || ownerId.equals(authentication.get().getName());
    }

    public boolean isAdminOrService() {
        Optional<Authentication> authentication = currentAuthentication();
        return authentication.isEmpty()
                || hasRole(authentication.get(), "ADMIN")
                || hasAnyServiceAuthority(authentication.get());
    }

    private Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return hasAuthority(authentication, "ROLE_" + role);
    }

    private boolean hasAnyServiceAuthority(Authentication authentication) {
        return hasAuthority(authentication, "SCOPE_internal")
                || hasAuthority(authentication, "SCOPE_service")
                || authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority -> authority.startsWith("ROLE_SERVICE_"));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
