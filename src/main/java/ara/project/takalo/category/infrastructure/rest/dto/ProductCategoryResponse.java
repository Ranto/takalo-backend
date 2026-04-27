package ara.project.takalo.category.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Représentation d'une catégorie de produit")
public record ProductCategoryResponse(
        @Schema(description = "Identifiant unique") UUID id,
        @Schema(description = "Libellé de la catégorie", example = "Produits laitiers") String label,
        @Schema(description = "Description de la catégorie", example = "Lait, yaourts, fromages") String description
) {
}
