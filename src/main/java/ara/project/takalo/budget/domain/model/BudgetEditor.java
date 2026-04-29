package ara.project.takalo.budget.domain.model;

import java.util.UUID;

public record BudgetEditor(UUID userId, String displayName, boolean creator) {
}
