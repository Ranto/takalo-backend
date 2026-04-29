package ara.project.takalo.budget.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Transfert d'un montant entre deux budgets. */
public record BudgetTransferCommand(
        UUID sourceBudgetId,
        UUID targetBudgetId,
        BigDecimal amount,
        Instant date,
        String reason
) {
}
