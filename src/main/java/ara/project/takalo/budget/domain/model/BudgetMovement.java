package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetMovement(
        UUID id,
        UUID budgetId,
        BudgetMovementType type,
        BigDecimal amount,
        Instant occurredAt,
        String reason,
        UUID correlationId,
        UUID purchaseId,
        UUID createdBy,
        Instant createdAt
) {
}
