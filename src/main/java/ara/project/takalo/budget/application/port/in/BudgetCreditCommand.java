package ara.project.takalo.budget.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

/** Crédit d'un budget depuis une source externe. */
public record BudgetCreditCommand(
        BigDecimal amount,
        Instant date,
        String reason
) {
}
