package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;

public record BudgetWithBalance(Budget budget, BigDecimal totalPurchases) {
}
