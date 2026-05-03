package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(UUID id,
                       UUID ownerId,
                       UUID budgetId,
                       Instant purchaseDate,
                       String notes,
                       Instant lockedAt,
                       UUID lockedBy,
                       List<PurchaseItem> items) {
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isLocked() {
        return lockedAt != null;
    }

    public Purchase withOwner(UUID newOwnerId) {
        return new Purchase(id, newOwnerId, budgetId, purchaseDate, notes, lockedAt, lockedBy, items);
    }

    public Purchase withBudget(UUID newBudgetId) {
        return new Purchase(id, ownerId, newBudgetId, purchaseDate, notes, lockedAt, lockedBy, items);
    }

    public Purchase withLock(Instant at, UUID by) {
        return new Purchase(id, ownerId, budgetId, purchaseDate, notes, at, by, items);
    }
}
