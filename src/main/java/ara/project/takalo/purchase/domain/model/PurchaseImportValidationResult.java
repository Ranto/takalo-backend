package ara.project.takalo.purchase.domain.model;

import java.util.List;

public record PurchaseImportValidationResult(
        List<PurchaseImportPreviewItem> validRows,
        List<ImportError> errors
) {
}
