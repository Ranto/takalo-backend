package ara.project.takalo.budget.infrastructure.rest.dto;

import java.util.UUID;

public record BudgetEditorResponse(UUID userId, String displayName, boolean creator) {
}
