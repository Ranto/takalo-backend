package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Résultat de la validation d'un import (aperçu sans persistance)")
public record PurchaseImportValidationResponse(
        @Schema(description = "Nombre de lignes valides qui seraient importées", example = "12") int validCount,
        @Schema(description = "Nombre de lignes en erreur", example = "2") int errorCount,
        @Schema(description = "Aperçu des lignes valides") List<PurchaseImportPreviewItemResponse> validRows,
        @Schema(description = "Erreurs ligne par ligne") List<ImportErrorResponse> errors
) {
}
