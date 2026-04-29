package ara.project.takalo.user.domain.model;

import java.util.UUID;

public record Permission(
        UUID id,
        String name,
        String description
) {
}
