package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Agrégat des dépenses par catégorie de produit")
public record CategorySpendingBreakdownResponse(
        @Schema(description = "Identifiant de la catégorie (null = produits sans catégorie)")
        UUID categoryId,
        @Schema(description = "Libellé de la catégorie (null = produits sans catégorie)")
        String categoryLabel,
        @Schema(description = "Total dépensé pour cette catégorie")
        BigDecimal total,
        @Schema(description = "Nombre de lignes d'achat agrégées")
        long itemCount
) {
}
