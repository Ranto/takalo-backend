package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(UUID id,
                       UUID ownerId,
                       UUID budgetId,
                       Instant purchaseDate,
                       List<PurchaseItem> items) {
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Purchase withOwner(UUID newOwnerId) {
        return new Purchase(id, newOwnerId, budgetId, purchaseDate, items);
    }

    public Purchase withBudget(UUID newBudgetId) {
        return new Purchase(id, ownerId, newBudgetId, purchaseDate, items);
    }
}
