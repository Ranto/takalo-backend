package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Détail d'une erreur survenue lors de l'import d'une ligne")
public record ImportErrorResponse(
        @Schema(description = "Numéro de ligne dans le fichier source", example = "5") Integer lineNumber,
        @Schema(description = "Valeur brute de la ligne en erreur") String rawValue,
        @Schema(description = "Message d'erreur explicatif", example = "Produit introuvable") String errorMessage
) {
}
