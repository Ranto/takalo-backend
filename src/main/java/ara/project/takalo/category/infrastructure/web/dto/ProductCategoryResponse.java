package ara.project.takalo.category.infrastructure.web.dto;

import java.util.UUID;

public record ProductCategoryResponse(UUID id, String label, String description) {
}
