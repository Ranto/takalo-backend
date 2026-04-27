package ara.project.takalo.product.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Données d'entrée pour la création ou la mise à jour d'un produit")
public record ProductRequest(
        @Schema(description = "Nom du produit", example = "Yaourt nature", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le nom du produit est obligatoire.")
        @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
        String name,

        @Schema(description = "Identifiant de la catégorie associée")
        UUID categoryId
) {
}
