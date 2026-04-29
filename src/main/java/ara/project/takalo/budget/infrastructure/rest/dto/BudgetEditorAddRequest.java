package ara.project.takalo.budget.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BudgetEditorAddRequest(@NotNull UUID userId) {
}
