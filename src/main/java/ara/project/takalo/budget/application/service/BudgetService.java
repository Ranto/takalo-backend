package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.application.port.in.BudgetTransferCommand;
import ara.project.takalo.budget.application.port.in.BudgetTransferResult;
import ara.project.takalo.budget.application.port.in.BudgetUpdateCommand;
import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetEditor;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.domain.model.BudgetTimeline;
import ara.project.takalo.budget.domain.model.BudgetTimelineEvent;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService implements BudgetServicePort {

    private final BudgetRepository repository;
    private final BudgetMovementRepository movementRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserServicePort userService;
    @Lazy
    private final UserDefaultBudgetServicePort defaultBudgetService;
    @Lazy
    private final PurchaseServicePort purchaseService;

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
        CreditExternalResult result = doCreditFromExternalSource(budgetId, command);
        return withBalance(result.budget());
    }

    @Override
    public BudgetMovement creditFromExternalSourceWithMovement(UUID budgetId, BudgetCreditCommand command) {
        return doCreditFromExternalSource(budgetId, command).movement();
    }

    private CreditExternalResult doCreditFromExternalSource(UUID budgetId, BudgetCreditCommand command) {
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

        BudgetMovement movement = movementRepository.save(new BudgetMovement(
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

        return new CreditExternalResult(saved, movement);
    }

    private record CreditExternalResult(Budget budget, BudgetMovement movement) {
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

    @Override
    public BudgetWithBalance addEditor(UUID budgetId, UUID userId) {
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        requireCreator(budget);
        userService.getById(userId);

        Set<UUID> editors = new HashSet<>(safeEditors(budget));
        if (!editors.add(userId)) {
            throw new AlreadyExistsException("L'utilisateur est déjà éditeur de ce budget");
        }
        Budget saved = repository.save(withEditors(budget, editors));
        return withBalance(saved);
    }

    @Override
    public BudgetWithBalance removeEditor(UUID budgetId, UUID userId) {
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        requireCreator(budget);
        if (userId.equals(budget.createdBy())) {
            throw new InvalidOperationException(
                    "Le créateur ne peut pas être retiré de la liste des éditeurs");
        }
        Set<UUID> editors = new HashSet<>(safeEditors(budget));
        if (!editors.remove(userId)) {
            throw new ResourceNotFoundException("L'utilisateur n'est pas éditeur de ce budget");
        }
        Budget saved = repository.save(withEditors(budget, editors));
        defaultBudgetService.clearForUserIfBudgetMatches(userId, budgetId);
        return withBalance(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetEditor> listEditors(UUID budgetId) {
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));
        UUID creatorId = budget.createdBy();
        return safeEditors(budget).stream()
                .map(uid -> {
                    User u = userService.getById(uid);
                    return new BudgetEditor(uid, u.displayName(), uid.equals(creatorId));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetTimeline findTimeline(UUID budgetId, Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidOperationException(
                    "La date de début doit être antérieure ou égale à la date de fin");
        }
        Budget budget = repository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget non trouvé"));

        List<BudgetMovement> movements = movementRepository.findByBudgetIdOrderByOccurredAtAsc(budgetId);

        BigDecimal originalFund = budget.initialFund();
        for (BudgetMovement m : movements) {
            if (m.type() == BudgetMovementType.CREDIT_EXTERNE
                    || m.type() == BudgetMovementType.TRANSFERT_ENTRANT
                    || m.type() == BudgetMovementType.TRANSFERT_SORTANT) {
                originalFund = originalFund.subtract(m.amount());
            }
        }

        Set<UUID> referencedPurchaseIds = movements.stream()
                .map(BudgetMovement::purchaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Purchase> currentlyLinked = purchaseService.findByBudgetId(budgetId);
        Map<UUID, Purchase> purchaseById = new HashMap<>();
        for (Purchase p : currentlyLinked) purchaseById.put(p.id(), p);
        if (!referencedPurchaseIds.isEmpty()) {
            for (Purchase p : purchaseService.findByIds(referencedPurchaseIds)) {
                purchaseById.putIfAbsent(p.id(), p);
            }
        }

        Set<UUID> initiallyHere = computeInitiallyHere(currentlyLinked, movements);

        List<BudgetTimelineEvent> events = new ArrayList<>();
        events.add(new BudgetTimelineEvent(
                budget.createdAt(), BudgetMovementType.CREATION,
                originalFund, null,
                null, null, null, null, null));

        for (BudgetMovement m : movements) {
            events.add(new BudgetTimelineEvent(
                    m.occurredAt(), m.type(), m.amount(), null,
                    m.reason(), m.correlationId(), m.purchaseId(),
                    m.source(), m.counterpartBudgetId()));
        }

        for (UUID pid : initiallyHere) {
            Purchase p = purchaseById.get(pid);
            if (p == null) continue;
            events.add(new BudgetTimelineEvent(
                    p.purchaseDate(), BudgetMovementType.ACHAT,
                    p.getTotalAmount().negate(), null,
                    null, null, p.id(), null, null));
        }

        events.sort(Comparator
                .comparing(BudgetTimelineEvent::date)
                .thenComparingInt(e -> creationFirst(e.type())));

        List<BudgetTimelineEvent> withBalance = new ArrayList<>(events.size());
        BigDecimal balance = BigDecimal.ZERO;
        boolean started = false;
        for (BudgetTimelineEvent e : events) {
            if (!started && e.type() == BudgetMovementType.CREATION) {
                balance = e.amount();
            } else {
                balance = balance.add(e.amount());
            }
            started = true;
            withBalance.add(new BudgetTimelineEvent(
                    e.date(), e.type(), e.amount(), balance,
                    e.reason(), e.correlationId(), e.purchaseId(),
                    e.source(), e.counterpartBudgetId()));
        }

        if (start == null && end == null) {
            return new BudgetTimeline(withBalance, null, null);
        }
        BigDecimal refStart = start == null ? null : balanceAt(withBalance, start);
        BigDecimal refEnd = end == null ? null : balanceAt(withBalance, end);
        List<BudgetTimelineEvent> filtered = withBalance.stream()
                .filter(e -> (start == null || !e.date().isBefore(start))
                        && (end == null || !e.date().isAfter(end)))
                .toList();
        return new BudgetTimeline(filtered, refStart, refEnd);
    }

    private static int creationFirst(BudgetMovementType t) {
        return t == BudgetMovementType.CREATION ? 0 : 1;
    }

    private static Set<UUID> computeInitiallyHere(List<Purchase> currentlyLinked,
                                                  List<BudgetMovement> movements) {
        Map<UUID, List<BudgetMovement>> byPurchase = movements.stream()
                .filter(m -> m.purchaseId() != null)
                .collect(Collectors.groupingBy(BudgetMovement::purchaseId));
        Set<UUID> result = new HashSet<>();
        for (Purchase p : currentlyLinked) {
            List<BudgetMovement> mv = byPurchase.getOrDefault(p.id(), List.of());
            boolean hasAssign = mv.stream()
                    .anyMatch(m -> m.type() == BudgetMovementType.ASSIGNATION_ACHAT);
            if (!hasAssign) {
                result.add(p.id());
            }
        }
        for (Map.Entry<UUID, List<BudgetMovement>> entry : byPurchase.entrySet()) {
            UUID pid = entry.getKey();
            if (result.contains(pid)) continue;
            BudgetMovementType firstType = entry.getValue().get(0).type();
            if (firstType == BudgetMovementType.DESASSIGNATION_ACHAT) {
                result.add(pid);
            }
        }
        return result;
    }

    private static BigDecimal balanceAt(List<BudgetTimelineEvent> events, Instant t) {
        BigDecimal last = BigDecimal.ZERO;
        for (BudgetTimelineEvent e : events) {
            if (e.date().isAfter(t)) break;
            last = e.remainingBalance();
        }
        return last;
    }

    private void requireCreator(Budget b) {
        if (!b.createdBy().equals(currentUserProvider.id())) {
            throw new ForbiddenException(
                    "Seul le créateur peut gérer la liste des éditeurs");
        }
    }

    private Budget withEditors(Budget b, Set<UUID> editors) {
        return new Budget(
                b.id(),
                b.name(),
                b.description(),
                b.initialFund(),
                b.createdBy(),
                editors,
                b.createdAt(),
                Instant.now()
        );
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
