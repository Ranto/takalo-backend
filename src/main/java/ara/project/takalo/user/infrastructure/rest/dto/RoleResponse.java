package ara.project.takalo.user.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Représentation d'un rôle RBAC")
public record RoleResponse(
        UUID id,
        String name,
        String description,
        Set<String> permissions
) {
}
