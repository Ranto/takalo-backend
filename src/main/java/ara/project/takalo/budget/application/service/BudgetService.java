package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.application.port.in.BudgetTransferCommand;
import ara.project.takalo.budget.application.port.in.BudgetTransferResult;
import ara.project.takalo.budget.application.port.in.BudgetUpdateCommand;
import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
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
    private final BudgetMovementRepository movementRepository;
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
        if (!safeEditors(existing).contains(currentUserId)) {
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
        return withBalance(saved);
    }

    @Override
    public void delete(UUID id) {
        Budget budget = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));

        UUID currentUserId = currentUserProvider.id();
        if (!safeEditors(budget).contains(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget");
        }

        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Budget getRawById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
    }

    @Override
    public void recordPurchaseAssignment(UUID budgetId, UUID purchaseId, BigDecimal amount,
                                         Instant date, String reason, UUID correlationId) {
        recordPurchaseMovement(BudgetMovementType.ASSIGNATION_ACHAT, budgetId, purchaseId,
                amount, date, reason, correlationId);
    }

    @Override
    public void recordPurchaseUnassignment(UUID budgetId, UUID purchaseId, BigDecimal amount,
                                           Instant date, String reason, UUID correlationId) {
        recordPurchaseMovement(BudgetMovementType.DESASSIGNATION_ACHAT, budgetId, purchaseId,
                amount, date, reason, correlationId);
    }

    private void recordPurchaseMovement(BudgetMovementType type, UUID budgetId, UUID purchaseId,
                                        BigDecimal amount, Instant date, String reason, UUID correlationId) {
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        UUID currentUserId = currentUserProvider.id();
        if (!safeEditors(budget).contains(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget");
        }
        BigDecimal signed = type == BudgetMovementType.ASSIGNATION_ACHAT ? amount.negate() : amount;
        BudgetMovement movement = new BudgetMovement(
                null,
                budgetId,
                type,
                signed,
                date,
                reason,
                correlationId,
                purchaseId,
                null,
                null,
                null,
                null
        );
        movementRepository.save(movement);
    }

    @Override
    public BudgetWithBalance creditFromExternalSource(UUID budgetId, BudgetCreditCommand command) {
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new InvalidOperationException("Le montant du crédit doit être strictement positif");
        }
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        UUID currentUserId = currentUserProvider.id();
        if (!safeEditors(budget).contains(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget");
        }

        Budget updated = withFund(budget, budget.initialFund().add(command.amount()));
        Budget saved = repository.save(updated);

        movementRepository.save(new BudgetMovement(
                null,
                saved.id(),
                BudgetMovementType.CREDIT_EXTERNE,
                command.amount(),
                command.date(),
                command.reason(),
                null,
                null,
                BudgetMovementSource.INCONNUE,
                null,
                null,
                null
        ));

        return withBalance(saved);
    }

    @Override
    public BudgetTransferResult transfer(BudgetTransferCommand cmd) {
        if (cmd.amount() == null || cmd.amount().signum() <= 0) {
            throw new InvalidOperationException("Le montant du transfert doit être strictement positif");
        }
        if (cmd.sourceBudgetId().equals(cmd.targetBudgetId())) {
            throw new InvalidOperationException("Le budget source et le budget cible doivent être différents");
        }

        Budget source = repository.findById(cmd.sourceBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget source non trouvé"));
        Budget target = repository.findById(cmd.targetBudgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget cible non trouvé"));

        UUID userId = currentUserProvider.id();
        if (!safeEditors(source).contains(userId) || !safeEditors(target).contains(userId)) {
            throw new ForbiddenException("Vous n'êtes pas éditeur de l'un des budgets");
        }

        Budget newSource = repository.save(withFund(source, source.initialFund().subtract(cmd.amount())));
        Budget newTarget = repository.save(withFund(target, target.initialFund().add(cmd.amount())));

        UUID correlationId = UUID.randomUUID();
        movementRepository.save(new BudgetMovement(
                null,
                newSource.id(),
                BudgetMovementType.TRANSFERT_SORTANT,
                cmd.amount().negate(),
                cmd.date(),
                cmd.reason(),
                correlationId,
                null,
                null,
                newTarget.id(),
                null,
                null
        ));
        movementRepository.save(new BudgetMovement(
                null,
                newTarget.id(),
                BudgetMovementType.TRANSFERT_ENTRANT,
                cmd.amount(),
                cmd.date(),
                cmd.reason(),
                correlationId,
                null,
                null,
                newSource.id(),
                null,
                null
        ));

        return new BudgetTransferResult(withBalance(newSource), withBalance(newTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetMovement> findMovements(UUID budgetId, BudgetMovementType typeFilter) {
        if (!repository.existsById(budgetId)) {
            throw new ResourceNotFoundException("Budget non trouvé");
        }
        return typeFilter == null
                ? movementRepository.findByBudgetIdOrderByOccurredAtAsc(budgetId)
                : movementRepository.findByBudgetIdAndTypeOrderByOccurredAtAsc(budgetId, typeFilter);
    }

    private static Set<UUID> safeEditors(Budget b) {
        return b.editorIds() == null ? Set.of() : b.editorIds();
    }

    private Budget withFund(Budget b, BigDecimal newFund) {
        return new Budget(
                b.id(),
                b.name(),
                b.description(),
                newFund,
                b.createdBy(),
                b.editorIds(),
                b.createdAt(),
                Instant.now()
        );
    }

    private BudgetWithBalance withBalance(Budget b) {
        BigDecimal total = repository.totalPurchasesByBudgetIds(List.of(b.id()))
                .getOrDefault(b.id(), BigDecimal.ZERO);
        return new BudgetWithBalance(b, total);
    }
}
