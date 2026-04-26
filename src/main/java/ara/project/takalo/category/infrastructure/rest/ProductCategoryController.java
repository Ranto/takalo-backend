package ara.project.takalo.category.infrastructure.rest;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryRequest;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryResponse;
import ara.project.takalo.category.infrastructure.rest.mapper.ProductCategoryWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import jakarta.validation.Valid;
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
    private final ProductCategoryWebMapper webMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategoryResponse create(@Valid @RequestBody ProductCategoryRequest request) {
        ProductCategory saved = service.create(webMapper.toDomain(request));
        return webMapper.toResponse(saved);
    }

    @GetMapping("/{id}")
    public ProductCategoryResponse getById(@PathVariable UUID id) {
        return webMapper.toResponse(service.getById(id));
    }

    @GetMapping
    public PagedResponse<ProductCategoryResponse> search(@RequestParam(required = false) String label,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return service.search(label, page, size).map(webMapper::toResponse);
    }

    @PutMapping("/{id}")
    public ProductCategoryResponse update(@PathVariable UUID id, @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategory updated = service.update(id, webMapper.toDomain(id, request));
        return webMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
