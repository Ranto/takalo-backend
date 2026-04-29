package ara.project.takalo.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record BudgetUpdateRequest(
        @NotBlank(message = "Le nom du budget est obligatoire")
        String name,
        String description,
        BigDecimal fond
) {
}
