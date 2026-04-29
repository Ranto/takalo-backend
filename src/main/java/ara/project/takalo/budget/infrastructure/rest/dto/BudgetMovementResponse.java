package ara.project.takalo.budget.infrastructure.rest.dto;

import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetMovementResponse(
        UUID id,
        BudgetMovementType type,
        BigDecimal montant,
        Instant date,
        String raison,
        UUID correlationId,
        BudgetMovementSource source,
        UUID counterpartBudgetId
) {
}
