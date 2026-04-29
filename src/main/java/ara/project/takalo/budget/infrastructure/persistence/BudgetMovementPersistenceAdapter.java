package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetMovementEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
