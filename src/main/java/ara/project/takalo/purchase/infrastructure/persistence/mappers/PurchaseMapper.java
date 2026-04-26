package ara.project.takalo.purchase.infrastructure.persistence.mappers;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseMapper {

    private final PurchaseItemMapper purchaseItemMapper;

    public Purchase toDomain(PurchaseEntity entity) {
        if (entity == null) return null;
        return new Purchase(
                entity.getId(),
                entity.getPurchaseDate(),
                entity.getItems().stream().map(purchaseItemMapper::toDomain).toList()
        );
    }

    public PurchaseEntity toEntity(Purchase domain) {
        if (domain == null) return null;
        PurchaseEntity entity = PurchaseEntity.builder()
                .id(domain.id())
                .purchaseDate(domain.purchaseDate())
                .build();

        if (domain.items() != null) {
            domain.items().stream()
                    .map(purchaseItemMapper::toEntity)
                    .forEach(entity::addItem);
        }

        return entity;
    }
}
