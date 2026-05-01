package ara.project.takalo.category.infrastructure.rest.mapper;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryRequest;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductCategoryWebMapper {

    public ProductCategory toDomain(ProductCategoryRequest request) {
        return new ProductCategory(null, request.label(), request.description());
    }

    public ProductCategory toDomain(UUID id, ProductCategoryRequest request) {
        return new ProductCategory(id, request.label(), request.description());
    }

    public ProductCategoryResponse toResponse(ProductCategory domain) {
        return toResponse(domain, 0L);
    }

    public ProductCategoryResponse toResponse(ProductCategory domain, long productCount) {
        return new ProductCategoryResponse(domain.id(), domain.label(), domain.description(), productCount);
    }
}
