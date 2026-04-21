package ara.project.takalo.product.infrastructure.rest.dto;

import java.util.UUID;

public record ProductResponse(UUID id, String name, ProductCategoryInfo category) {
}
