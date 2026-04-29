package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.application.port.in.BudgetUpdateCommand;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService implements BudgetServicePort {

    private final BudgetRepository repository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Budget create(Budget budget) {
        if (repository.existsByName(budget.name())) {
            throw new AlreadyExistsException("Un budget avec ce nom existe déjà");
        }
        UUID creatorId = currentUserProvider.id();
        Set<UUID> editorIds = new HashSet<>();
        editorIds.add(creatorId);

        Budget toSave = new Budget(
                null,
                budget.name(),
                budget.description(),
                budget.initialFund(),
                creatorId,
                editorIds,
                null,
                null
        );
        return repository.save(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetWithBalance getById(UUID id) {
        Budget budget = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        BigDecimal total = repository.totalPurchasesByBudgetIds(List.of(id))
                .getOrDefault(id, BigDecimal.ZERO);
        return new BudgetWithBalance(budget, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BudgetWithBalance> findAll(int page, int size) {
        PagedResponse<Budget> budgets = repository.findAll(page, size);
        List<UUID> ids = budgets.content().stream().map(Budget::id).toList();
        Map<UUID, BigDecimal> totals = ids.isEmpty()
                ? Map.of()
                : repository.totalPurchasesByBudgetIds(ids);
        return budgets.map(b -> new BudgetWithBalance(b, totals.getOrDefault(b.id(), BigDecimal.ZERO)));
    }

    @Override
    public BudgetWithBalance update(UUID id, BudgetUpdateCommand command) {
        if (command.fond() != null && command.fond().isPresent()) {
            throw new InvalidOperationException(
                    "Le fond initial d'un budget ne peut pas être modifié, utiliser un crédit");
        }

        Budget existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));

        UUID currentUserId = currentUserProvider.id();
        Set<UUID> editorIds = existing.editorIds() == null ? Set.of() : existing.editorIds();
        if (!editorIds.contains(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget");
        }

        if (!existing.name().equals(command.name()) && repository.existsByName(command.name())) {
            throw new AlreadyExistsException("Un budget avec ce nom existe déjà");
        }

        Budget toSave = new Budget(
                existing.id(),
                command.name(),
                command.description(),
                existing.initialFund(),
                existing.createdBy(),
                existing.editorIds(),
                existing.createdAt(),
                Instant.now()
        );
        Budget saved = repository.save(toSave);
        BigDecimal total = repository.totalPurchasesByBudgetIds(List.of(saved.id()))
                .getOrDefault(saved.id(), BigDecimal.ZERO);
        return new BudgetWithBalance(saved, total);
    }
}
