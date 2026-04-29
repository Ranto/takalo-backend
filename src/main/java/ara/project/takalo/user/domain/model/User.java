package ara.project.takalo.user.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record User(
        UUID id,
        String externalId,
        String email,
        String displayName,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public User {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public Set<String> permissionNames() {
        return roles.stream()
                .flatMap(r -> r.permissions().stream())
                .map(Permission::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> roleNames() {
        return roles.stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
    }
}
