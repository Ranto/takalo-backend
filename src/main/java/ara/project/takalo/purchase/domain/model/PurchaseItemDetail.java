package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseItemDetail(UUID itemId,
                                 UUID purchaseId,
                                 Instant purchaseDate,
                                 UUID productId,
                                 String productName,
                                 UUID categoryId,
                                 String categoryLabel,
                                 UUID budgetId,
                                 String budgetName,
                                 BigDecimal unitPrice,
                                 Double quantity,
                                 BigDecimal discount,
                                 String storeName) {

    public BigDecimal total() {
        BigDecimal disc = discount == null ? BigDecimal.ZERO : discount;
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(disc);
    }
}