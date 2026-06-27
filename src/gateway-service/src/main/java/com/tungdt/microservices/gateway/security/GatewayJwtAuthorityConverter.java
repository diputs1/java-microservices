package com.tungdt.microservices.gateway.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class GatewayJwtAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
    private final String clientId;

    public GatewayJwtAuthorityConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
        addRealmRoles(jwt, authorities);
        addClientRoles(jwt, authorities);
        return authorities;
    }

    private void addRealmRoles(Jwt jwt, List<GrantedAuthority> authorities) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return;
        }
        addRoles(realmAccess.get("roles"), authorities);
    }

    private void addClientRoles(Jwt jwt, List<GrantedAuthority> authorities) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }
        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            return;
        }
        addRoles(clientAccessMap.get("roles"), authorities);
    }

    private void addRoles(Object roles, List<GrantedAuthority> authorities) {
        if (!(roles instanceof Collection<?> roleCollection)) {
            return;
        }
        roleCollection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }
}
