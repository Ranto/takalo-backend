package ara.project.takalo.product.application.port.in;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductServicePort {

    Product create(Product product);

    Product update(UUID id, Product product);

    void delete(UUID id);

    Product getById(UUID id);

    PagedResponse<Product> searchByCategoriesOrName(List<UUID> categoryIds, String name, boolean includeUncategorized, int page, int limit);

    PagedResponse<Product> findAll(int page, int size);

    Map<UUID, String> getProductNames(Set<UUID> ids);

    Optional<UUID> findIdByName(String name);

    Product findOrCreateByName(String name);

    Map<UUID, Long> countByCategoryIds(Set<UUID> categoryIds);
}
