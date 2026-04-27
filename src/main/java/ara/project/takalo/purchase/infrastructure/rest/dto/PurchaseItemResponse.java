package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Article d'un achat tel que renvoyé par l'API")
public record PurchaseItemResponse(
        @Schema(description = "Identifiant du produit") UUID productId,
        @Schema(description = "Quantité achetée", example = "2.0") Double quantity,
        @Schema(description = "Prix unitaire", example = "2.50") BigDecimal unitPrice,
        @Schema(description = "Remise appliquée", example = "0.50") BigDecimal discount,
        @Schema(description = "Date de péremption", example = "2026-12-31") LocalDate expiryDate,
        @Schema(description = "Nom du magasin", example = "Carrefour Antananarivo") String storeName,
        @Schema(description = "Nom du produit", example = "Yaourt nature") String productName
) {
}
