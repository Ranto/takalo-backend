package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseImportPreviewItem(
        int lineNumber,
        LocalDate purchaseDate,
        String productName,
        UUID productId,
        boolean productExists,
        String categoryName,
        UUID categoryId,
        boolean categoryExists,
        Double quantity,
        BigDecimal unitPrice,
        BigDecimal discount
) {
}
