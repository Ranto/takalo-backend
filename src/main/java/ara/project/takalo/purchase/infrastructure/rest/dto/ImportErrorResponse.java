package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Détail d'une erreur survenue lors de l'import d'une ligne")
public record ImportErrorResponse(
        @Schema(description = "Numéro de ligne dans le fichier source", example = "5") Integer lineNumber,
        @Schema(description = "Valeur brute du champ en cause (null si la valeur est absente)") String rawValue,
        @Schema(description = "Message d'erreur explicatif", example = "Produit introuvable") String errorMessage,
        @Schema(description = "Date d'achat lue") LocalDate purchaseDate,
        @Schema(description = "Nom du produit lu") String productName,
        @Schema(description = "Quantité lue") Double quantity,
        @Schema(description = "Prix unitaire lu") BigDecimal unitPrice,
        @Schema(description = "Nom de catégorie lu") String categoryName
) {
}
