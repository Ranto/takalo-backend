package ara.project.takalo.category.application.port.in;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductCategoryServicePort {
    ProductCategory create(ProductCategory category);

    ProductCategory update(UUID id, ProductCategory category);

    void delete(UUID id);

    ProductCategory getById(UUID id);

    PagedResponse<ProductCategory> search(String label, int page, int limit);

    Map<UUID, String> getCategoryLabels(Set<UUID> ids);

    ProductCategory findOrCreateByLabel(String label);

    Optional<UUID> findIdByLabel(String label);
}
