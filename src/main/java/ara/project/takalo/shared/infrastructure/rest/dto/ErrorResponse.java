package ara.project.takalo.shared.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Réponse standard renvoyée pour toutes les erreurs HTTP")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Code HTTP", example = "404") int status,
        @Schema(description = "Message d'erreur (en français)", example = "Catégorie introuvable") String message,
        @Schema(description = "Horodatage de l'erreur") LocalDateTime timestamp,
        @Schema(description = "Chemin de la requête", example = "/api/v1/categories/123") String path,
        @Schema(
                description = "Erreurs de validation par champ (présent uniquement pour les erreurs 400 de validation)",
                example = "{\"name\": \"Le nom est obligatoire\"}",
                nullable = true
        ) Map<String, String> fieldErrors
) {
    public ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
        this(status, message, timestamp, path, null);
    }
}
