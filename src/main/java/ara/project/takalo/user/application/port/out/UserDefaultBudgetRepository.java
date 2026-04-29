package ara.project.takalo.user.application.port.out;

import ara.project.takalo.user.domain.model.UserDefaultBudget;

import java.util.Optional;
import java.util.UUID;

public interface UserDefaultBudgetRepository {

    Optional<UserDefaultBudget> findByUserId(UUID userId);

    UserDefaultBudget save(UserDefaultBudget pref);
}
