package ara.project.takalo.purchase.infrastructure.persistence.mappers;

import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseItemEntity;
import org.springframework.stereotype.Component;

@Component
public class PurchaseItemMapper {

    public PurchaseItem toDomain(PurchaseItemEntity entity) {
        if (entity == null) return null;
        return new PurchaseItem(
                entity.getId(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getDiscount(),
                entity.getExpiryDate(),
                entity.getStoreName(),
                entity.getProductName()
        );
    }

    public PurchaseItemEntity toEntity(PurchaseItem domain) {
        if (domain == null) return null;
        return PurchaseItemEntity.builder()
                .productId(domain.productId())
                .discount(domain.discount())
                .expiryDate(domain.expiryDate())
                .quantity(domain.quantity())
                .unitPrice(domain.unitPrice())
                .storeName(domain.storeName())
                .build();
    }
}
