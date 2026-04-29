package ara.project.takalo.user.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Représentation d'un utilisateur")
public record UserResponse(
        UUID id,
        String externalId,
        String email,
        String displayName,
        Set<String> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
