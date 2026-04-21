package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.domain.repository.ProductCategoryRepository;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}
