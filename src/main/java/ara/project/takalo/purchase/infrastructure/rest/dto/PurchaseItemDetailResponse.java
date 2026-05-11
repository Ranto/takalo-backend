package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Ligne détaillée d'un achat (vue à plat) avec produit et catégorie courants")
public record PurchaseItemDetailResponse(
        @Schema(description = "Identifiant de la ligne d'achat") UUID itemId,
        @Schema(description = "Identifiant de l'achat parent") UUID purchaseId,
        @Schema(description = "Date de l'achat") Instant date,
        @Schema(description = "Identifiant du produit") UUID productId,
        @Schema(description = "Nom du produit (snapshot au moment de l'achat)") String productName,
        @Schema(description = "Identifiant de la catégorie courante du produit (null si produit supprimé ou sans catégorie)")
        UUID categoryId,
        @Schema(description = "Libellé de la catégorie courante (null si produit supprimé ou sans catégorie)")
        String categoryName,
        @Schema(description = "Identifiant du budget de l'achat parent (null si sans budget)") UUID budgetId,
        @Schema(description = "Nom du budget de l'achat parent (null si sans budget ou budget supprimé)") String budgetName,
        @Schema(description = "Prix unitaire") BigDecimal unitPrice,
        @Schema(description = "Quantité achetée") Double quantity,
        @Schema(description = "Remise appliquée") BigDecimal discount,
        @Schema(description = "Total de la ligne (unitPrice * quantity - discount)") BigDecimal total,
        @Schema(description = "Nom du magasin") String storeName
) {
}
