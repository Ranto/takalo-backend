package ara.project.takalo.user.domain.model;

import java.util.Set;
import java.util.UUID;

public record Role(
        UUID id,
        String name,
        String description,
        Set<Permission> permissions
) {
    public Role {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
