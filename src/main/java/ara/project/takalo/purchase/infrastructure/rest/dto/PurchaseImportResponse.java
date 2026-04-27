package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Résultat d'un import d'achats : combinaison de lignes importées et d'erreurs détaillées")
public record PurchaseImportResponse(
        @Schema(description = "Nombre d'achats importés avec succès", example = "12") int importedCount,
        @Schema(description = "Nombre de lignes en erreur", example = "2") int errorCount,
        @Schema(description = "Achats créés") List<PurchaseResponse> imported,
        @Schema(description = "Erreurs ligne par ligne") List<ImportErrorResponse> errors
) {
}
