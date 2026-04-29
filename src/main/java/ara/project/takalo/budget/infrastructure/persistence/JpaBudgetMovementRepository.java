package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaBudgetMovementRepository extends JpaRepository<BudgetMovementEntity, UUID> {
}
