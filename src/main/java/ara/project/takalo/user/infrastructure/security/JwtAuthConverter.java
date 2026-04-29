package ara.project.takalo.user.infrastructure.security;

import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Convertit un JWT en Authentication enrichie des rôles/permissions Takalo.
 * Provisionne l'utilisateur en DB lors du premier appel (JIT).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserServicePort userServicePort;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String externalId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String displayName = jwt.getClaimAsString("name");
        if (displayName == null) displayName = jwt.getClaimAsString("preferred_username");
        if (email == null) email = externalId + "@unknown.local";

        User user = userServicePort.provision(externalId, email, displayName);

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String role : user.roleNames()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String perm : user.permissionNames()) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
        }
        return new JwtAuthenticationToken(jwt, authorities, externalId);
    }
}
