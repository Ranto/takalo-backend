package ara.project.takalo.purchase.application.port.in;

import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.domain.model.PurchaseImportValidationResult;

import java.io.InputStream;

public interface PurchaseImportServicePort {
    PurchaseImportResult importPurchases(InputStream source, ImportFormat format);

    PurchaseImportValidationResult validate(InputStream source, ImportFormat format);
}
