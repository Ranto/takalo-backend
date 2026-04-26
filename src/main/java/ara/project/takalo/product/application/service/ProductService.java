package ara.project.takalo.product.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.domain.repository.ProductRepository;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductServicePort {

    private final ProductRepository repository;

    @Override
    public Product create(Product product) {
        if (repository.existsByName(product.name())) {
            throw new AlreadyExistsException("Un produit avec ce nom existe déjà");
        }
        return repository.save(product);
    }

    @Override
    public Product update(UUID id, Product product) {
        return repository.findById(id).map(existing -> {
            boolean usedName = repository.existsByName(product.name());
            if (usedName && !product.name().equals(existing.name())) {
                throw new AlreadyExistsException("Un produit avec ce nom existe déjà");
            }

            Product toUpdate = new Product(id,
                    product.name(),
                    product.categoryId(),
                    product.categoryLabel(),
                    null,
                    null);
            return repository.save(toUpdate);
        }).orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Product> getById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public PagedResponse<Product> searchByCategoriesOrName(List<UUID> categoryIds, String name, int page, int limit) {
        return repository.findByNameOrCategoryIds(name, categoryIds, page, limit);
    }

    @Override
    public PagedResponse<Product> findAll(int page, int size) {
        return repository.findAll(page, size);
    }

    @Override
    public Map<UUID, String> getProductNames(Set<UUID> ids) {
        return repository.getProductNames(ids);
    }
}
