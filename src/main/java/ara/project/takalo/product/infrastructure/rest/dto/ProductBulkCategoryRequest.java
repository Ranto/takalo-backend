package ara.project.takalo.product.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Requête de mise à jour en masse de la catégorie de plusieurs produits")
public record ProductBulkCategoryRequest(
        @Schema(description = "Identifiants des produits à modifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La liste des produits est obligatoire.")
        List<UUID> productIds,

        @Schema(description = "Identifiant de la catégorie à assigner. null pour retirer la catégorie.")
        UUID categoryId
) {
}