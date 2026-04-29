package ara.project.takalo.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DefaultBudgetRequest(@NotNull UUID budgetId) {
}
