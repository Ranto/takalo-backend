package ara.project.takalo.product.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Vue allégée d'une catégorie embarquée dans un produit")
public record ProductCategoryInfo(
        @Schema(description = "Identifiant de la catégorie") UUID id,
        @Schema(description = "Libellé de la catégorie", example = "Produits laitiers") String label
) {
}
