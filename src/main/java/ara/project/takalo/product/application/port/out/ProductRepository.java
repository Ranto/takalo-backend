package ara.project.takalo.product.application.port.out;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.product.domain.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(UUID id);

    PagedResponse<Product> findAll(int page, int size);

    void deleteById(UUID id);

    PagedResponse<Product> findByNameOrCategoryIds(String name, List<UUID> categoryIds, boolean includeUncategorized, int page, int size);

    boolean existsByName(String name);

    Map<UUID, String> getProductNames(Set<UUID> productIds);

    Optional<UUID> findIdByName(String name);

    Map<UUID, Long> countByCategoryIds(Set<UUID> categoryIds);

    int updateCategoryByIds(List<UUID> productIds, UUID categoryId);
}
