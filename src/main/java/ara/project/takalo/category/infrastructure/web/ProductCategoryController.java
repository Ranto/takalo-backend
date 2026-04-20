package ara.project.takalo.category.infrastructure.web;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.domain.utility.PagedResponse;
import ara.project.takalo.category.infrastructure.web.dto.ProductCategoryRequest;
import ara.project.takalo.category.infrastructure.web.dto.ProductCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductCategoryServicePort service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategoryResponse create(@RequestBody ProductCategoryRequest request) {
        ProductCategory domainToSave = new ProductCategory(null, request.label(), request.description());
        ProductCategory saved = service.create(domainToSave);
        return mapToResponse(saved);
    }

    @GetMapping("/{id}")
    public ProductCategoryResponse getById(@PathVariable UUID id) {
        return mapToResponse(service.getById(id));
    }

    @GetMapping
    public PagedResponse<ProductCategoryResponse> search(@RequestParam(required = false) String label,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return service.search(label, page, size).map(this::mapToResponse);
    }

    @PutMapping("/{id}")
    public ProductCategoryResponse update(@PathVariable UUID id, @RequestBody ProductCategoryRequest request) {
        ProductCategory domain = new ProductCategory(id, request.label(), request.description());
        return mapToResponse(service.update(id, domain));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private ProductCategoryResponse mapToResponse(ProductCategory domain) {
        return new ProductCategoryResponse(domain.id(), domain.label(), domain.description());
    }
}
