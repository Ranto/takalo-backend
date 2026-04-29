package ara.project.takalo.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserDefaultBudget(UUID userId, UUID budgetId, Instant updatedAt) {

    public static UserDefaultBudget empty(UUID userId) {
        return new UserDefaultBudget(userId, null, Instant.now());
    }

    public boolean isSet() {
        return budgetId != null;
    }
}
