package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaBudgetMovementRepository extends JpaRepository<BudgetMovementEntity, UUID> {

    List<BudgetMovementEntity> findByBudgetIdOrderByOccurredAtAsc(UUID budgetId);

    List<BudgetMovementEntity> findByBudgetIdAndTypeOrderByOccurredAtAsc(UUID budgetId, BudgetMovementType type);
}
