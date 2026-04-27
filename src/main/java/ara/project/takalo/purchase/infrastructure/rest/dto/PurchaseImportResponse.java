package ara.project.takalo.purchase.infrastructure.rest.dto;

import java.util.List;

public record PurchaseImportResponse(
        int importedCount,
        int errorCount,
        List<PurchaseResponse> imported,
        List<ImportErrorResponse> errors
) {
}