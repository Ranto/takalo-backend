package ara.project.takalo.product.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Représentation d'un produit")
public record ProductResponse(
        @Schema(description = "Identifiant unique") UUID id,
        @Schema(description = "Nom du produit", example = "Yaourt nature") String name,
        @Schema(description = "Catégorie associée") ProductCategoryInfo category
) {
}
