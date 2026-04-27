package ara.project.takalo.shared.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Réponse standard renvoyée pour toutes les erreurs HTTP")
public record ErrorResponse(
        @Schema(description = "Code HTTP", example = "404") int status,
        @Schema(description = "Message d'erreur (en français)", example = "Catégorie introuvable") String message,
        @Schema(description = "Horodatage de l'erreur") LocalDateTime timestamp,
        @Schema(description = "Chemin de la requête", example = "/api/v1/categories/123") String path
) {
}
