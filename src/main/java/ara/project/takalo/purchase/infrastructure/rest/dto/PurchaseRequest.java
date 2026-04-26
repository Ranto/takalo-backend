package ara.project.takalo.purchase.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;
import java.util.List;

public record PurchaseRequest(
        @NotNull(message = "La date d'achat est obligatoire.")
        @PastOrPresent(message = "La date d'achat ne peut pas être dans le futur")
        Instant purchaseDate,

        @NotEmpty(message = "L'achat doit contenir au moins un article.")
        @Valid
        List<PurchaseItemRequest> items
) {

}
