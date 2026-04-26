package ara.project.takalo.purchase.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseLightResponse(
        UUID id,
        Instant purchaseDate,
        int itemCount,
        BigDecimal totalAmount
) {
}
