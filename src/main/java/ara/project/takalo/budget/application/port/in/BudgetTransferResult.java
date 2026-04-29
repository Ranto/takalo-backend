package ara.project.takalo.budget.application.port.in;

import ara.project.takalo.budget.domain.model.BudgetWithBalance;

/** Résultat d'un transfert : état (avec reste) du budget source et cible après opération. */
public record BudgetTransferResult(
        BudgetWithBalance source,
        BudgetWithBalance target
) {
}
