package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat d'une réassignation en masse d'achats à un budget")
public record BulkReassignPurchaseBudgetResponse(
        @Schema(description = "Nombre d'achats réassignés") int updatedCount,
        @Schema(description = "Identifiant de corrélation partagé par les mouvements de ledger générés") UUID correlationId,
        @Schema(description = "Achats réassignés (vue allégée)") List<PurchaseLightResponse> purchases
) {
}
