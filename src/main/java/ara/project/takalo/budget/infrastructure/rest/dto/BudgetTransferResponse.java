package ara.project.takalo.budget.infrastructure.rest.dto;

public record BudgetTransferResponse(
        BudgetResponse source,
        BudgetResponse target
) {
}
