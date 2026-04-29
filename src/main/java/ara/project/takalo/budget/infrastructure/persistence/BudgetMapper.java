package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEditorEntity;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BudgetMapper {

    public Budget toDomain(BudgetEntity entity) {
        if (entity == null) return null;
        Set<UUID> editorIds = entity.getEditors() == null
                ? Set.of()
                : entity.getEditors().stream().map(BudgetEditorEntity::getUserId).collect(Collectors.toUnmodifiableSet());
        return new Budget(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getInitialFund(),
                entity.getCreatedBy(),
                editorIds,
                entity.getCreatedAt(),
                entity.getModifiedAt()
        );
    }

    public BudgetEntity toEntity(Budget domain) {
        if (domain == null) return null;
        Set<BudgetEditorEntity> editors = new HashSet<>();
        if (domain.editorIds() != null) {
            Instant now = Instant.now();
            for (UUID userId : domain.editorIds()) {
                editors.add(BudgetEditorEntity.builder()
                        .userId(userId)
                        .addedAt(now)
                        .build());
            }
        }
        return BudgetEntity.builder()
                .id(domain.id())
                .name(domain.name())
                .description(domain.description())
                .initialFund(domain.initialFund())
                .editors(editors)
                .build();
    }
}
