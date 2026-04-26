package ara.project.takalo.purchase.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseItemResponse(UUID productId,
                                   Double quantity,
                                   BigDecimal unitPrice,
                                   BigDecimal discount,
                                   LocalDate expiryDate,
                                   String storeName,
                                   String productName) {
}
