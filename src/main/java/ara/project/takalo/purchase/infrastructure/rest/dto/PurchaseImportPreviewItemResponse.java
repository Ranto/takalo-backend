package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Aperçu d'une ligne valide qui serait importée")
public record PurchaseImportPreviewItemResponse(
        @Schema(description = "Numéro de ligne dans le fichier source", example = "5") int lineNumber,
        @Schema(description = "Date de l'achat") LocalDate purchaseDate,
        @Schema(description = "Nom du produit") String productName,
        @Schema(description = "Identifiant du produit existant, null si à créer") UUID productId,
        @Schema(description = "Vrai si le produit existe déjà, faux s'il sera créé à l'import") boolean productExists,
        @Schema(description = "Nom de la catégorie (peut être absent)") String categoryName,
        @Schema(description = "Identifiant de la catégorie existante, null si à créer ou non fournie") UUID categoryId,
        @Schema(description = "Vrai si la catégorie existe déjà, faux si elle sera créée ou non fournie") boolean categoryExists,
        @Schema(description = "Quantité achetée") Double quantity,
        @Schema(description = "Prix unitaire") BigDecimal unitPrice,
        @Schema(description = "Remise appliquée") BigDecimal discount
) {
}
