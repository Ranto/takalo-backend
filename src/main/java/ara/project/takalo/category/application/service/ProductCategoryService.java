package ara.project.takalo.category.application.service;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.application.port.out.ProductCategoryRepository;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductCategoryService implements ProductCategoryServicePort {
    private final ProductCategoryRepository repository;

    @Override
    public ProductCategory create(ProductCategory category) {
        if (repository.existsByLabel(category.label())) {
            throw new AlreadyExistsException("Une catégorie avec ce nom existe déjà.");
        }
        return repository.save(category);
    }

    @Override
    public ProductCategory update(UUID id, ProductCategory category) {
        return repository.findById(id)
                .map(existing -> {
                    ProductCategory updated = new ProductCategory(id, category.label(), category.description());
                    return repository.save(updated);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductCategory getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<ProductCategory> search(String label, int page, int limit) {
        if (label == null || label.isBlank()) {
            return repository.findAll(page, limit);
        }
        return repository.findByLabel(label, page, limit);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<UUID, String> getCategoryLabels(Set<UUID> ids) {
        return repository.getCategoryLabels(ids);
    }
}
