package ara.project.takalo.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetCreditRequest(
        @NotNull(message = "Le montant du crédit est obligatoire")
        BigDecimal montant,
        @NotNull(message = "La date du crédit est obligatoire")
        Instant date,
        @NotBlank(message = "La raison du crédit est obligatoire")
        String raison
) {
}
