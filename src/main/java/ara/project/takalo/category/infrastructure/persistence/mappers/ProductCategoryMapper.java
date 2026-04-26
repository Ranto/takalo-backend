package ara.project.takalo.category.infrastructure.persistence.mappers;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryMapper {
    public ProductCategoryEntity toEntity(ProductCategory domain) {
        if (domain == null) {
            return null;
        }
        return ProductCategoryEntity.builder()
                .id(domain.id())
                .label(domain.label())
                .description(domain.description())
                .build();
    }

    public ProductCategory toDomain(ProductCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProductCategory(entity.getId(), entity.getLabel(), entity.getDescription());
    }
}
