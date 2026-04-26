package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param productName Snapshot du nom du produit au moment de l'achat. Figé volontairement
 *                    pour rester stable même si le produit est renommé ou supprimé par la suite.
 */
public record PurchaseItem(UUID productId,
                           Double quantity,
                           BigDecimal unitPrice,
                           BigDecimal discount,
                           LocalDate expiryDate,
                           String storeName,
                           String productName) {
    public BigDecimal getTotalAmount() {
        BigDecimal discountValue = discount == null ? BigDecimal.ZERO : discount;
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discountValue);
    }
}
