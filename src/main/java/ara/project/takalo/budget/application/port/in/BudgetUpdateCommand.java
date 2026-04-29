package ara.project.takalo.budget.application.port.in;

import java.math.BigDecimal;
import java.util.Optional;

public record BudgetUpdateCommand(
        String name,
        String description,
        Optional<BigDecimal> fond
) {
}
