package ara.project.takalo.budget.application.port.out;

import ara.project.takalo.budget.domain.model.BudgetMovement;

public interface BudgetMovementRepository {
    BudgetMovement save(BudgetMovement movement);
}
