package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetMovementEntity;
import org.springframework.stereotype.Component;

@Component
public class BudgetMovementMapper {

    public BudgetMovement toDomain(BudgetMovementEntity entity) {
        if (entity == null) return null;
        return new BudgetMovement(
                entity.getId(),
                entity.getBudgetId(),
                entity.getType(),
                entity.getAmount(),
                entity.getOccurredAt(),
                entity.getReason(),
                entity.getCorrelationId(),
                entity.getPurchaseId(),
                entity.getSource(),
                entity.getCounterpartBudgetId(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public BudgetMovementEntity toEntity(BudgetMovement domain) {
        if (domain == null) return null;
        return BudgetMovementEntity.builder()
                .id(domain.id())
                .budgetId(domain.budgetId())
                .type(domain.type())
                .amount(domain.amount())
                .occurredAt(domain.occurredAt())
                .reason(domain.reason())
                .correlationId(domain.correlationId())
                .purchaseId(domain.purchaseId())
                .source(domain.source())
                .counterpartBudgetId(domain.counterpartBudgetId())
                .build();
    }
}
