package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetTimelineEvent(
        Instant date,
        BudgetMovementType type,
        BigDecimal amount,
        BigDecimal remainingBalance,
        String reason,
        UUID correlationId,
        UUID purchaseId,
        BudgetMovementSource source,
        UUID counterpartBudgetId
) {
}
