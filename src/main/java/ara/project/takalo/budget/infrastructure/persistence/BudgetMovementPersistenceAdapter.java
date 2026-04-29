package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetMovementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BudgetMovementPersistenceAdapter implements BudgetMovementRepository {

    private final JpaBudgetMovementRepository jpaRepository;
    private final BudgetMovementMapper mapper;

    @Override
    public BudgetMovement save(BudgetMovement movement) {
        BudgetMovementEntity entity = mapper.toEntity(movement);
        BudgetMovementEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<BudgetMovement> findByBudgetIdOrderByOccurredAtAsc(UUID budgetId) {
        return jpaRepository.findByBudgetIdOrderByOccurredAtAsc(budgetId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<BudgetMovement> findByBudgetIdAndTypeOrderByOccurredAtAsc(UUID budgetId, BudgetMovementType type) {
        return jpaRepository.findByBudgetIdAndTypeOrderByOccurredAtAsc(budgetId, type).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
