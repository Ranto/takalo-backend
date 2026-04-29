package ara.project.takalo.user.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.application.port.out.UserDefaultBudgetRepository;
import ara.project.takalo.user.domain.model.UserDefaultBudget;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserDefaultBudgetService implements UserDefaultBudgetServicePort {

    private final UserDefaultBudgetRepository repository;
    @Lazy
    private final BudgetServicePort budgetService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public UserDefaultBudget getCurrent() {
        UUID userId = currentUserProvider.id();
        return repository.findByUserId(userId)
                .orElseGet(() -> UserDefaultBudget.empty(userId));
    }

    @Override
    public UserDefaultBudget setCurrent(UUID budgetId) {
        UUID userId = currentUserProvider.id();
        Budget budget = budgetService.getRawById(budgetId);
        Set<UUID> editors = budget.editorIds() == null ? Set.of() : budget.editorIds();
        if (!editors.contains(userId)) {
            throw new ForbiddenException(
                    "Le budget par défaut doit être un budget dont vous êtes éditeur");
        }
        return repository.save(new UserDefaultBudget(userId, budgetId, Instant.now()));
    }

    @Override
    public UserDefaultBudget clearCurrent() {
        UUID userId = currentUserProvider.id();
        return repository.save(UserDefaultBudget.empty(userId));
    }

    @Override
    public void clearForUserIfBudgetMatches(UUID userId, UUID budgetId) {
        repository.findByUserId(userId).ifPresent(pref -> {
            if (budgetId.equals(pref.budgetId())) {
                repository.save(UserDefaultBudget.empty(userId));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> resolveDefaultBudgetIdFor(UUID userId) {
        return repository.findByUserId(userId).map(UserDefaultBudget::budgetId);
    }
}
