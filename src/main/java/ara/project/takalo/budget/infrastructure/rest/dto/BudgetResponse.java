package ara.project.takalo.budget.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        String name,
        String description,
        BigDecimal fond,
        BigDecimal totalAchats,
        BigDecimal reste,
        UUID createdBy,
        Set<UUID> editorIds,
        Instant createdAt,
        Instant modifiedAt
) {
}
