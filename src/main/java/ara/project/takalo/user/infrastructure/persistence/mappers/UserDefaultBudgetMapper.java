package ara.project.takalo.user.infrastructure.persistence.mappers;

import ara.project.takalo.user.domain.model.UserDefaultBudget;
import ara.project.takalo.user.infrastructure.persistence.entities.UserDefaultBudgetEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDefaultBudgetMapper {

    public UserDefaultBudget toDomain(UserDefaultBudgetEntity entity) {
        return new UserDefaultBudget(entity.getUserId(), entity.getBudgetId(), entity.getUpdatedAt());
    }

    public UserDefaultBudgetEntity toEntity(UserDefaultBudget pref) {
        return UserDefaultBudgetEntity.builder()
                .userId(pref.userId())
                .budgetId(pref.budgetId())
                .updatedAt(pref.updatedAt())
                .build();
    }
}
