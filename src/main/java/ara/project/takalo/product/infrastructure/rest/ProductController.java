package ara.project.takalo.product.infrastructure.rest;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import ara.project.takalo.product.infrastructure.rest.mapper.ProductWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductServicePort productServicePort;
    private final ProductWebMapper webMapper;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        var domain = webMapper.toDomain(request);
        var savedProduct = productServicePort.create(domain);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(savedProduct));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return productServicePort.getById(id)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var pagedDomain = productServicePort.findAll(page, size);
        PagedResponse<ProductResponse> response = webMapper.toResponses(pagedDomain);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<Product> pagedDomain = productServicePort.searchByCategoriesOrName(categoryIds, name, page, size);
        PagedResponse<ProductResponse> response = webMapper.toResponses(pagedDomain);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @RequestBody ProductRequest request) {
        Product toUpdate = new Product(id,
                request.name(),
                request.categoryId(),
                null,
                null);
        Product updated = productServicePort.update(id, toUpdate);
        return webMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productServicePort.delete(id);
    }
}
