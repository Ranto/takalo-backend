package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Données pour réassigner un achat à un autre budget")
public record ReassignPurchaseBudgetRequest(
        @Schema(description = "Identifiant du nouveau budget", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le budget cible est obligatoire.")
        UUID budgetId,

        @Schema(description = "Date métier de la réassignation (ISO-8601)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La date est obligatoire.")
        Instant date,

        @Schema(description = "Raison de la réassignation", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La raison est obligatoire.")
        String raison
) {
}
