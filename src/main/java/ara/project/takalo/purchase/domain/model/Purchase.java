package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(UUID id,
                       Instant purchaseDate,
                       List<PurchaseItem> items) {
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(PurchaseItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
