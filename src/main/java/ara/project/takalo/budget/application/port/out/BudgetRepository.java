package ara.project.takalo.budget.application.port.out;

import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);

    Optional<Budget> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByName(String name);

    /** Renvoie pour chaque id la somme des montants d'achats associés. */
    Map<UUID, BigDecimal> totalPurchasesByBudgetIds(Collection<UUID> budgetIds);

    PagedResponse<Budget> findAll(int page, int size);

    void deleteById(UUID id);
}
