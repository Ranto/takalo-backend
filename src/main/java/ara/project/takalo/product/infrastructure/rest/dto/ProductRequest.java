package ara.project.takalo.product.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Le nom du produit est obligatoire.")
        @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
        String name,
        UUID categoryId
) {
}
