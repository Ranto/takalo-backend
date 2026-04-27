package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;
import java.util.List;

@Schema(description = "Données d'entrée pour la création ou la mise à jour d'un achat")
public record PurchaseRequest(
        @Schema(description = "Date d'achat (ISO-8601)", example = "2026-04-01T10:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La date d'achat est obligatoire.")
        @PastOrPresent(message = "La date d'achat ne peut pas être dans le futur")
        Instant purchaseDate,

        @Schema(description = "Articles de l'achat", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "L'achat doit contenir au moins un article.")
        @Valid
        List<PurchaseItemRequest> items
) {

}
