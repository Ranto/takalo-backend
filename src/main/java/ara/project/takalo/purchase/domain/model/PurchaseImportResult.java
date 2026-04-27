package ara.project.takalo.purchase.domain.model;

import java.util.List;

public record PurchaseImportResult(List<Purchase> imported, List<ImportError> errors) {
}
