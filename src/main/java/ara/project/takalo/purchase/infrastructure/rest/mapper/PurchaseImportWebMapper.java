package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.ImportError;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;
import ara.project.takalo.purchase.domain.model.PurchaseImportPreviewItem;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.domain.model.PurchaseImportValidationResult;
import ara.project.takalo.purchase.infrastructure.rest.dto.ImportErrorResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportPreviewItemResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseImportWebMapper {

    private final PurchaseWebMapper purchaseWebMapper;

    public PurchaseImportResponse toResponse(PurchaseImportResult result) {
        return new PurchaseImportResponse(
                result.imported().size(),
                result.errors().size(),
                result.imported().stream().map(purchaseWebMapper::toResponse).toList(),
                result.errors().stream()
                        .map(this::toErrorResponse)
                        .toList()
        );
    }

    public PurchaseImportValidationResponse toValidationResponse(PurchaseImportValidationResult result) {
        return new PurchaseImportValidationResponse(
                result.validRows().size(),
                result.errors().size(),
                result.validRows().stream().map(this::toPreviewResponse).toList(),
                result.errors().stream()
                        .map(this::toErrorResponse)
                        .toList()
        );
    }

    private ImportErrorResponse toErrorResponse(ImportError error) {
        ParsedPurchaseRow row = error.row();
        return new ImportErrorResponse(
                error.lineNumber(),
                error.rawValue(),
                error.errorMessage(),
                row == null ? null : row.purchaseDate(),
                row == null ? null : row.productName(),
                row == null ? null : row.quantity(),
                row == null ? null : row.unitPrice(),
                row == null ? null : row.categoryName()
        );
    }

    private PurchaseImportPreviewItemResponse toPreviewResponse(PurchaseImportPreviewItem item) {
        return new PurchaseImportPreviewItemResponse(
                item.lineNumber(),
                item.purchaseDate(),
                item.productName(),
                item.productId(),
                item.productExists(),
                item.categoryName(),
                item.categoryId(),
                item.categoryExists(),
                item.quantity(),
                item.unitPrice(),
                item.discount()
        );
    }
}
