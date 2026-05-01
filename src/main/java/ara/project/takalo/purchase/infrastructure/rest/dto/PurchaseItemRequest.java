package ara.project.takalo.purchase.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Article d'un achat")
public record PurchaseItemRequest(
        @Schema(description = "Nom du produit acheté. Si aucun produit ne porte ce nom (correspondance exacte, "
                + "insensible à la casse), il sera créé automatiquement avec une catégorie vide.",
                example = "Pâtes complètes", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le nom du produit est obligatoire.")
        @Size(min = 1, max = 100, message = "Le nom du produit doit contenir entre 1 et 100 caractères.")
        String productName,

        @Schema(description = "Prix unitaire", example = "2.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Le prix unitaire est obligatoire.")
        @PositiveOrZero(message = "Le prix unitaire ne peut pas être négatif.")
        BigDecimal unitPrice,

        @Schema(description = "Quantité achetée", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être positive")
        Double quantity,

        @Schema(description = "Remise appliquée (optionnelle)", example = "0.50")
        @PositiveOrZero(message = "La remise ne peut être négatif.")
        BigDecimal discount,

        @Schema(description = "Date de péremption (aujourd'hui ou future)", example = "2026-12-31")
        @FutureOrPresent(message = "La date de péremption doit être aujourd'hui ou dans le futur")
        LocalDate expiryDate,

        @Schema(description = "Nom du magasin", example = "Carrefour Antananarivo")
        String storeName
) {
}
