package ara.project.takalo.budget.application.port.out;

import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementType;

import java.util.List;
import java.util.UUID;

public interface BudgetMovementRepository {
    BudgetMovement save(BudgetMovement movement);

    List<BudgetMovement> findByBudgetIdOrderByOccurredAtAsc(UUID budgetId);

    List<BudgetMovement> findByBudgetIdAndTypeOrderByOccurredAtAsc(UUID budgetId, BudgetMovementType type);
}
