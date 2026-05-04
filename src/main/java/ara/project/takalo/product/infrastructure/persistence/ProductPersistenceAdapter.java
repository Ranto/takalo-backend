package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.application.port.out.ProductRepository;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import ara.project.takalo.product.infrastructure.persistence.mappers.ProductMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductRepository {

    private final JpaProductRepository repository;
    private final ProductMapper productMapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = productMapper.toEntity(product);
        return productMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id)
                .map(productMapper::toDomain);
    }

    @Override
    public PagedResponse<Product> findAll(int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entityPage = repository.findAllWithNameOrCategoryIdIn(null, null, false, false, pageable);

        return PaginationMapper.toPagedResponse(entityPage, productMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public PagedResponse<Product> findByNameOrCategoryIds(String name, List<UUID> categoryIds, boolean includeUncategorized, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<UUID> normalizedIds = (categoryIds == null || categoryIds.isEmpty()) ? null : categoryIds;
        boolean hasCategoryFilter = normalizedIds != null || includeUncategorized;
        Page<ProductEntity> entityPage = repository.findAllWithNameOrCategoryIdIn(
                name, normalizedIds, includeUncategorized, hasCategoryFilter, pageable);
        return PaginationMapper.toPagedResponse(entityPage, productMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public Optional<UUID> findIdByName(String name) {
        return repository.findIdByName(name);
    }

    @Override
    public Map<UUID, String> getProductNames(Set<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        return repository.findNamesById(productIds).stream()
                .collect(Collectors.toMap(
                        JpaProductRepository.ProductIdAndName::getId,
                        JpaProductRepository.ProductIdAndName::getName
                ));
    }

    @Override
    public int updateCategoryByIds(List<UUID> productIds, UUID categoryId) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        return repository.updateCategoryByIds(productIds, categoryId);
    }

    @Override
    public Map<UUID, Long> countByCategoryIds(Set<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByCategoryIds(categoryIds).stream()
                .collect(Collectors.toMap(
                        JpaProductRepository.CategoryProductCount::getCategoryId,
                        JpaProductRepository.CategoryProductCount::getCount
                ));
    }
}
