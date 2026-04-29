package ara.project.takalo.user.infrastructure.rest.dto;

import java.util.UUID;

public record DefaultBudgetResponse(UUID budgetId, String budgetName) {
}
