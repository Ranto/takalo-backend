package ara.project.takalo.category.application.port.in;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.domain.utility.PagedResponse;

import java.util.UUID;

public interface ProductCategoryServicePort {
    ProductCategory create(ProductCategory category);

    ProductCategory update(UUID id, ProductCategory category);

    void delete(UUID id);

    ProductCategory getById(UUID id);

    PagedResponse<ProductCategory> search(String label, int page, int limit);
}
