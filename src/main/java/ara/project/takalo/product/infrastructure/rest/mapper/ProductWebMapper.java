package ara.project.takalo.product.infrastructure.rest.mapper;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductCategoryInfo;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductWebMapper {

    private final ProductCategoryServicePort categoryServicePort;

    public Product toDomain(ProductRequest request) {
        return new Product(
                null,
                request.name(),
                request.categoryId(),
                null,
                null
        );
    }

    public ProductResponse toResponse(Product domain) {
        String label = domain.categoryId() == null
                ? null
                : categoryServicePort.getCategoryLabels(Set.of(domain.categoryId())).get(domain.categoryId());
        return buildResponse(domain, label);
    }

    public PagedResponse<ProductResponse> toResponses(PagedResponse<Product> page) {
        Set<UUID> categoryIds = page.content().stream()
                .map(Product::categoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> labels = categoryIds.isEmpty()
                ? Map.of()
                : categoryServicePort.getCategoryLabels(categoryIds);
        return page.map(p -> buildResponse(p, p.categoryId() == null ? null : labels.get(p.categoryId())));
    }

    private ProductResponse buildResponse(Product domain, String categoryLabel) {
        return new ProductResponse(
                domain.id(),
                domain.name(),
                new ProductCategoryInfo(domain.categoryId(), categoryLabel)
        );
    }
}
