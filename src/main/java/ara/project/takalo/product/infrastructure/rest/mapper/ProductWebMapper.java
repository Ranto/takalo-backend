package ara.project.takalo.product.infrastructure.rest.mapper;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductCategoryInfo;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductWebMapper {

    public Product toDomain(ProductRequest request) {
        return new Product(
                null,
                request.name(),
                request.categoryId(),
                null,
                null,
                null
        );
    }

    public ProductResponse toResponse(Product domain) {
        return new ProductResponse(
                domain.id(),
                domain.name(),
                new ProductCategoryInfo(domain.categoryId(), domain.categoryLabel())
        );
    }
}
