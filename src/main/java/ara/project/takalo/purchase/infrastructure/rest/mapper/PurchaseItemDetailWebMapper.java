package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class PurchaseItemDetailWebMapper {

    public PurchaseItemDetailResponse toResponse(PurchaseItemDetail domain) {
        return new PurchaseItemDetailResponse(
                domain.itemId(),
                domain.purchaseId(),
                domain.purchaseDate(),
                domain.productId(),
                domain.productName(),
                domain.categoryId(),
                domain.categoryLabel(),
                domain.unitPrice(),
                domain.quantity(),
                domain.discount(),
                domain.total(),
                domain.storeName()
        );
    }
}
