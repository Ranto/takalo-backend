package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vue allégée d'un achat (sans le détail des articles), utilisée pour les listings")
public record PurchaseLightResponse(
        @Schema(description = "Identifiant unique de l'achat") UUID id,
        @Schema(description = "Identifiant du budget associé (peut être null)") UUID budgetId,
        @Schema(description = "Date d'achat", example = "2026-04-01T10:30:00Z") Instant purchaseDate,
        @Schema(description = "Nombre d'articles", example = "3") int itemCount,
        @Schema(description = "Montant total", example = "12.50") BigDecimal totalAmount
) {
}
