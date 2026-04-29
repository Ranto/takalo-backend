package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Représentation détaillée d'un achat avec ses articles")
public record PurchaseResponse(
        @Schema(description = "Identifiant unique de l'achat") UUID id,
        @Schema(description = "Identifiant du budget associé (peut être null)") UUID budgetId,
        @Schema(description = "Date d'achat", example = "2026-04-01T10:30:00Z") Instant purchaseDate,
        @Schema(description = "Articles de l'achat") List<PurchaseItemResponse> items,
        @Schema(description = "Montant total après remises", example = "12.50") BigDecimal totalAmount
) {
}
