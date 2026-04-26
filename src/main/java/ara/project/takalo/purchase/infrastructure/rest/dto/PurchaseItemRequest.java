package ara.project.takalo.purchase.infrastructure.rest.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseItemRequest(
        @NotNull(message = "L'identifiant du produit est obligatoire.")
        UUID productId,

        @NotNull(message = "Le prix unitaire est obligatoire.")
        @PositiveOrZero(message = "Le prix unitaire ne peut pas être négatif.")
        BigDecimal unitPrice,

        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être positive")
        Double quantity,

        @PositiveOrZero(message = "La remise ne peut être négatif.")
        BigDecimal discount,

        @FutureOrPresent(message = "La date de péremption doit être aujourd'hui ou dans le futur")
        LocalDate expiryDate,

        String storeName,

        String productName
) {
}
