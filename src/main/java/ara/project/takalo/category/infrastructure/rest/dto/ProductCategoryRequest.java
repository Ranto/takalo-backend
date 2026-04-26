package ara.project.takalo.category.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCategoryRequest(
        @NotBlank(message = "Le libellé de la catégorie est obligatoire.")
        @Size(min = 2, max = 100, message = "Le libellé doit contenir entre 2 et 100 caractères")
        String label,
        String description
) {
}
