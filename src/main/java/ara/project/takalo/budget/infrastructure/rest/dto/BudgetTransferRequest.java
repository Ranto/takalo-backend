package ara.project.takalo.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetTransferRequest(
        @NotNull(message = "Le budget source est obligatoire")
        UUID sourceBudgetId,
        @NotNull(message = "Le budget cible est obligatoire")
        UUID targetBudgetId,
        @NotNull(message = "Le montant du transfert est obligatoire")
        BigDecimal montant,
        @NotNull(message = "La date du transfert est obligatoire")
        Instant date,
        @NotBlank(message = "La raison du transfert est obligatoire")
        String raison
) {
}
