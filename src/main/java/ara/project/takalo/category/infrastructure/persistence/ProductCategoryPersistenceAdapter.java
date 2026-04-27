package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.application.port.out.ProductCategoryRepository;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.category.infrastructure.persistence.mappers.ProductCategoryMapper;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProductCategoryPersistenceAdapter implements ProductCategoryRepository {

    private final JpaProductCategoryRepository repository;
    private final ProductCategoryMapper mapper;

    @Override
    public ProductCategory save(ProductCategory productCategory) {
        ProductCategoryEntity entity = mapper.toEntity(productCategory);
        ProductCategoryEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProductCategory> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResponse<ProductCategory> findByLabel(String label, int page, int size) {
        Page<ProductCategoryEntity> resultPage = repository.findByLabelContainingIgnoreCase(label, PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(resultPage, mapper::toDomain);
    }

    @Override
    public PagedResponse<ProductCategory> findAll(int page, int size) {
        Page<ProductCategoryEntity> resultPage = repository.findAll(PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(resultPage, mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public Map<UUID, String> getCategoryLabels(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return repository.findLabelsById(ids).stream()
                .collect(Collectors.toMap(
                        JpaProductCategoryRepository.CategoryIdAndLabel::getId,
                        JpaProductCategoryRepository.CategoryIdAndLabel::getLabel
                ));
    }

    @Override
    public boolean existsByLabel(String label) {
        return repository.existsByLabelIgnoreCase(label);
    }
}
