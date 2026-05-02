package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Données pour réassigner plusieurs achats à un même budget en une seule opération")
public record BulkReassignPurchaseBudgetRequest(
        @Schema(description = "Identifiants des achats à réassigner", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La liste des achats est obligatoire.")
        @Size(max = 200, message = "Maximum 200 achats par opération.")
        List<UUID> purchaseIds,

        @Schema(description = "Identifiant du budget cible", requiredMode = Schema.RequiredMode.REQUIRED)
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
