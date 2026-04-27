package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedPurchaseRow(
        int lineNumber,
        LocalDate purchaseDate,
        String productName,
        Double quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        LocalDate expiryDate,
        String storeName
) {
}
