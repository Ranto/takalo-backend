package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toDomain(ProductEntity entity) {
        if (entity.getCategory() == null) {
            return new Product(entity.getId(),
                    entity.getName(),
                    null,
                    null,
                    entity.getCreatedAt(),
                    entity.getUpdatedAt());
        }
        return new Product(entity.getId(),
                entity.getName(),
                entity.getCategory().getId(),
                entity.getCategory().getLabel(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ProductEntity toEntity(Product domain, ProductCategoryEntity category) {
        return ProductEntity.builder()
                .id(domain.id())
                .name(domain.name())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .category(category)
                .build();
    }
}
