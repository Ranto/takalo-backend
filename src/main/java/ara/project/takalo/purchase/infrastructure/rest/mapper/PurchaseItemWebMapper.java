package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PurchaseItemWebMapper {
    
    public PurchaseItem toDomain(PurchaseItemRequest request) {
        return new PurchaseItem(
                null,
                request.quantity(),
                request.unitPrice(),
                request.discount() == null ? BigDecimal.ZERO : request.discount(),
                request.expiryDate(),
                request.storeName(),
                request.productName()
        );
    }

    public PurchaseItemResponse toResponse(PurchaseItem domain) {
        return new PurchaseItemResponse(
                domain.productId(),
                domain.quantity(),
                domain.unitPrice(),
                domain.discount(),
                domain.expiryDate(),
                domain.storeName(),
                domain.productName()
        );
    }
}
