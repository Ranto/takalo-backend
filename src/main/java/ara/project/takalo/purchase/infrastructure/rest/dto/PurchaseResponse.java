package ara.project.takalo.purchase.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseResponse(UUID id, Instant purchaseDate, List<PurchaseItemResponse> items, BigDecimal totalAmount) {
}
