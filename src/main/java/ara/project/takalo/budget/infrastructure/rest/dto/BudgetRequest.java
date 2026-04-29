package ara.project.takalo.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank(message = "Le nom du budget est obligatoire")
        String name,
        String description,
        @NotNull(message = "Le fond initial est obligatoire")
        BigDecimal fond
) {
}
