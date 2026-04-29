package ara.project.takalo.budget.application.port.in;

import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.util.UUID;

public interface BudgetServicePort {
    Budget create(Budget budget);

    BudgetWithBalance getById(UUID id);

    PagedResponse<BudgetWithBalance> findAll(int page, int size);
}
