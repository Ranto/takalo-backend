package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Budget(
        UUID id,
        String name,
        String description,
        BigDecimal initialFund,
        UUID createdBy,
        Set<UUID> editorIds,
        Instant createdAt,
        Instant modifiedAt
) {
}
