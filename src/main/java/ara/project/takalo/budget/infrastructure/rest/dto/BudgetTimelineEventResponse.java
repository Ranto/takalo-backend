package ara.project.takalo.budget.infrastructure.rest.dto;

import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetTimelineEventResponse(
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
