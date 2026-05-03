package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Liste d'identifiants d'achats pour une opération en lot (verrouillage / déverrouillage)")
public record BulkPurchaseIdsRequest(
        @Schema(description = "Identifiants des achats", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La liste des achats est obligatoire.")
        @Size(max = 200, message = "Maximum 200 achats par opération.")
        List<UUID> purchaseIds
) {
}
