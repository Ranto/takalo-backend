package ara.project.takalo.product.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        UUID categoryId,
        String categoryLabel,
        Instant createdAt,
        Instant updatedAt
) {

}
