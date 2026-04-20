package ara.project.takalo.category.domain.repository;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.domain.utility.PagedResponse;

import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository {
    ProductCategory save(ProductCategory productCategory);
    Optional<ProductCategory> findById(UUID id);
    PagedResponse<ProductCategory> findByLabel(String label, int page, int size);
    PagedResponse<ProductCategory> findAll(int page, int size);
    void deleteById(UUID id);
}
