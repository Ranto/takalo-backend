package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(UUID id,
                       UUID ownerId,
                       Instant purchaseDate,
                       List<PurchaseItem> items) {
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Purchase withOwner(UUID newOwnerId) {
        return new Purchase(id, newOwnerId, purchaseDate, items);
    }
}
