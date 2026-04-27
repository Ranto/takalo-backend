package ara.project.takalo.category.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données d'entrée pour la création ou la mise à jour d'une catégorie")
public record ProductCategoryRequest(
        @Schema(description = "Libellé unique de la catégorie", example = "Produits laitiers", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le libellé de la catégorie est obligatoire.")
        @Size(min = 2, max = 100, message = "Le libellé doit contenir entre 2 et 100 caractères")
        String label,

        @Schema(description = "Description libre de la catégorie", example = "Lait, yaourts, fromages")
        String description
) {
}
