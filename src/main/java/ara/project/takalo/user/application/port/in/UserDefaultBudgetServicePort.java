package ara.project.takalo.user.application.port.in;

import ara.project.takalo.user.domain.model.UserDefaultBudget;

import java.util.Optional;
import java.util.UUID;

public interface UserDefaultBudgetServicePort {

    UserDefaultBudget getCurrent();

    UserDefaultBudget setCurrent(UUID budgetId);

    UserDefaultBudget clearCurrent();

    void clearForUserIfBudgetMatches(UUID userId, UUID budgetId);

    Optional<UUID> resolveDefaultBudgetIdFor(UUID userId);
}
