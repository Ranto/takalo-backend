package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.infrastructure.rest.dto.ImportErrorResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportResponse;
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
                        .map(e -> new ImportErrorResponse(e.lineNumber(), e.rawValue(), e.errorMessage()))
                        .toList()
        );
    }
}