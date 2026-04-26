package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.domain.repository.ProductRepository;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @Override
    public Product save(Product product) {
        ProductCategoryEntity categoryEntity = null;
        if (product.categoryId() != null) {
            categoryEntity = entityManager.getReference(ProductCategoryEntity.class, product.categoryId());
        }
        ProductEntity entity = productMapper.toEntity(product, categoryEntity);
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
        var entityPage = repository.findAllWithNameOrCategoryIdIn(null, null, pageable);

        return PaginationMapper.toPagedResponse(entityPage, productMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }

    @Override
    public PagedResponse<Product> findByNameOrCategoryIds(String name, List<UUID> categoryIds, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ProductEntity> entityPage = repository.findAllWithNameOrCategoryIdIn(name, categoryIds, pageable);
        return PaginationMapper.toPagedResponse(entityPage, productMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
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
}
