package ara.project.takalo.verification.application.service;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.verification.application.port.in.RegularizationCommand;
import ara.project.takalo.verification.application.port.in.VerificationCreateCommand;
import ara.project.takalo.verification.application.port.in.VerificationServicePort;
import ara.project.takalo.verification.application.port.out.VerificationRepository;
import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.RegularizationKind;
import ara.project.takalo.verification.domain.model.Verification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class VerificationService implements VerificationServicePort {

    private static final String PERM_READ_ANY = "PERM_verification:read:any";
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final VerificationRepository repository;
    private final BudgetServicePort budgetService;
    private final PurchaseServicePort purchaseService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Verification create(VerificationCreateCommand command) {
        validateAmounts(command);
        validateDenominationsSum(command);

        Budget budget = budgetService.getRawById(command.budgetId());
        UUID currentUserId = currentUserProvider.id();
        if (!safeEditors(budget).contains(currentUserId)) {
            throw new ForbiddenException("Vous n'êtes pas éditeur de ce budget");
        }

        BigDecimal difference = command.countedTotal().subtract(command.referenceRest());
        RegularizationCommand regularization = command.regularization();

        UUID regularizationPurchaseId = null;
        UUID regularizationCreditMovementId = null;
        RegularizationKind kind = RegularizationKind.NONE;

        if (regularization != null && regularization.kind() != null
                && regularization.kind() != RegularizationKind.NONE) {
            validateRegularization(regularization, difference);
            kind = regularization.kind();
            if (kind == RegularizationKind.PURCHASE) {
                Purchase saved = purchaseService.create(buildRegularizationPurchase(command, regularization, difference));
                regularizationPurchaseId = saved.id();
            } else {
                BudgetMovement movement = budgetService.creditFromExternalSourceWithMovement(
                        command.budgetId(),
                        new BudgetCreditCommand(
                                difference,
                                regularization.date().atStartOfDay(ZoneOffset.UTC).toInstant(),
                                regularization.label()
                        )
                );
                regularizationCreditMovementId = movement.id();
            }
        }

        Verification toSave = new Verification(
                null,
                command.budgetId(),
                currentUserId,
                command.verificationDate(),
                trimToNull(command.note()),
                command.referenceRest(),
                command.countedTotal(),
                difference,
                command.denominations(),
                kind,
                regularizationPurchaseId,
                regularizationCreditMovementId,
                null,
                null
        );
        return repository.save(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Verification> search(UUID budgetId, LocalDate start, LocalDate end, int page, int size) {
        UUID ownerFilter = (currentUserProvider.hasAuthority(PERM_READ_ANY)
                || currentUserProvider.hasAuthority(ROLE_SUPER_ADMIN))
                ? null
                : currentUserProvider.id();
        return repository.search(budgetId, start, end, ownerFilter, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Verification getById(UUID id) {
        Verification existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vérification non trouvée"));
        UUID currentUserId = currentUserProvider.id();
        if (existing.ownerId() != null && existing.ownerId().equals(currentUserId)) {
            return existing;
        }
        if (currentUserProvider.hasAuthority(PERM_READ_ANY)
                || currentUserProvider.hasAuthority(ROLE_SUPER_ADMIN)) {
            return existing;
        }
        Budget budget = budgetService.getRawById(existing.budgetId());
        if (!safeEditors(budget).contains(currentUserId)) {
            throw new ForbiddenException("Accès refusé à cette vérification");
        }
        return existing;
    }

    private void validateAmounts(VerificationCreateCommand cmd) {
        if (cmd.referenceRest() == null || cmd.referenceRest().signum() < 0) {
            throw new InvalidOperationException("Le reste de référence doit être positif ou nul");
        }
        if (cmd.countedTotal() == null || cmd.countedTotal().signum() < 0) {
            throw new InvalidOperationException("Le total compté doit être positif ou nul");
        }
        if (cmd.denominations() == null) {
            throw new InvalidOperationException("La liste des coupures est obligatoire");
        }
    }

    private void validateDenominationsSum(VerificationCreateCommand cmd) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DenominationCount dc : cmd.denominations()) {
            if (dc.value() <= 0) {
                throw new InvalidOperationException("La valeur d'une coupure doit être strictement positive");
            }
            if (dc.quantity() < 0) {
                throw new InvalidOperationException("La quantité d'une coupure doit être positive ou nulle");
            }
            sum = sum.add(BigDecimal.valueOf((long) dc.value() * dc.quantity()));
        }
        if (sum.compareTo(cmd.countedTotal()) != 0) {
            throw new InvalidOperationException(
                    "Le total compté ne correspond pas à la somme des coupures saisies");
        }
    }

    private void validateRegularization(RegularizationCommand regularization, BigDecimal difference) {
        if (regularization.label() == null || regularization.label().isBlank()) {
            throw new InvalidOperationException("Le libellé de régularisation est obligatoire");
        }
        if (regularization.date() == null) {
            throw new InvalidOperationException("La date de régularisation est obligatoire");
        }
        if (regularization.kind() == RegularizationKind.PURCHASE && difference.signum() >= 0) {
            throw new InvalidOperationException(
                    "Une régularisation par achat n'est possible qu'en cas de manquant");
        }
        if (regularization.kind() == RegularizationKind.CREDIT && difference.signum() <= 0) {
            throw new InvalidOperationException(
                    "Une régularisation par crédit n'est possible qu'en cas d'excédent");
        }
    }

    private Purchase buildRegularizationPurchase(VerificationCreateCommand command,
                                                  RegularizationCommand regularization,
                                                  BigDecimal difference) {
        PurchaseItem item = new PurchaseItem(
                null,
                1.0d,
                difference.abs(),
                BigDecimal.ZERO,
                null,
                null,
                regularization.label()
        );
        return new Purchase(
                null,
                null,
                command.budgetId(),
                regularization.date().atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC),
                regularization.label(),
                null,
                null,
                List.of(item)
        );
    }

    private static Set<UUID> safeEditors(Budget budget) {
        return budget.editorIds() == null ? Set.of() : budget.editorIds();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
