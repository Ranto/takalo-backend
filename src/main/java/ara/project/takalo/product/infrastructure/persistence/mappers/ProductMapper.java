package ara.project.takalo.product.infrastructure.persistence.mappers;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toDomain(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Product(entity.getId(),
                entity.getName(),
                entity.getCategoryId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ProductEntity toEntity(Product domain) {
        if (domain == null) {
            return null;
        }

        return ProductEntity.builder()
                .id(domain.id())
                .name(domain.name())
                .categoryId(domain.categoryId())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
