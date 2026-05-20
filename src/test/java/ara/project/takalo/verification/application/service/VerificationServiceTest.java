package ara.project.takalo.verification.application.service;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.verification.application.port.in.RegularizationCommand;
import ara.project.takalo.verification.application.port.in.VerificationCreateCommand;
import ara.project.takalo.verification.application.port.out.VerificationRepository;
import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.RegularizationKind;
import ara.project.takalo.verification.domain.model.Verification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationRepository repository;
    @Mock
    private BudgetServicePort budgetService;
    @Mock
    private PurchaseServicePort purchaseService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private VerificationService service;

    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Budget budgetWithEditor() {
        return new Budget(
                BUDGET_ID,
                "Caisse",
                "",
                new BigDecimal("100000.00"),
                USER_ID,
                Set.of(USER_ID),
                Instant.now(),
                null
        );
    }

    private VerificationCreateCommand commandBalanced() {
        return new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                "ok",
                new BigDecimal("12000.00"),
                new BigDecimal("12000.00"),
                List.of(new DenominationCount(10000, 1), new DenominationCount(1000, 2)),
                null
        );
    }

    @Test
    void create_persistsVerification_withoutRegularization() {
        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithEditor());
        when(repository.save(any(Verification.class))).thenAnswer(inv -> inv.getArgument(0));

        Verification result = service.create(commandBalanced());

        ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
        verify(repository).save(captor.capture());
        Verification saved = captor.getValue();
        assertThat(saved.difference()).isEqualByComparingTo("0");
        assertThat(saved.regularizationKind()).isEqualTo(RegularizationKind.NONE);
        assertThat(saved.regularizationPurchaseId()).isNull();
        assertThat(saved.regularizationCreditMovementId()).isNull();
        assertThat(saved.ownerId()).isEqualTo(USER_ID);
        assertThat(result).isSameAs(saved);
        verify(purchaseService, never()).create(any());
        verify(budgetService, never()).creditFromExternalSourceWithMovement(any(), any());
    }

    @Test
    void create_withPurchaseRegularization_createsPurchaseAndStoresId() {
        VerificationCreateCommand cmd = new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                null,
                new BigDecimal("12000.00"),
                new BigDecimal("11500.00"),
                List.of(new DenominationCount(10000, 1), new DenominationCount(1000, 1), new DenominationCount(500, 1)),
                new RegularizationCommand(RegularizationKind.PURCHASE, "Manquant caisse", LocalDate.now())
        );
        UUID purchaseId = UUID.randomUUID();
        Purchase savedPurchase = new Purchase(purchaseId, USER_ID, BUDGET_ID, Instant.now(), "Manquant caisse", null, null, List.of());

        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithEditor());
        when(purchaseService.create(any(Purchase.class))).thenReturn(savedPurchase);
        when(repository.save(any(Verification.class))).thenAnswer(inv -> inv.getArgument(0));

        Verification result = service.create(cmd);

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseService).create(purchaseCaptor.capture());
        assertThat(purchaseCaptor.getValue().items()).hasSize(1);
        assertThat(purchaseCaptor.getValue().items().getFirst().unitPrice()).isEqualByComparingTo("500.00");
        assertThat(purchaseCaptor.getValue().items().getFirst().productName()).isEqualTo("Manquant caisse");
        assertThat(result.regularizationKind()).isEqualTo(RegularizationKind.PURCHASE);
        assertThat(result.regularizationPurchaseId()).isEqualTo(purchaseId);
        assertThat(result.regularizationCreditMovementId()).isNull();
        verify(budgetService, never()).creditFromExternalSourceWithMovement(any(), any());
    }

    @Test
    void create_withCreditRegularization_createsCreditAndStoresMovementId() {
        VerificationCreateCommand cmd = new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("12000.00"),
                List.of(new DenominationCount(10000, 1), new DenominationCount(1000, 2)),
                new RegularizationCommand(RegularizationKind.CREDIT, "Excédent caisse", LocalDate.now())
        );
        UUID movementId = UUID.randomUUID();
        BudgetMovement movement = new BudgetMovement(
                movementId, BUDGET_ID, BudgetMovementType.CREDIT_EXTERNE,
                new BigDecimal("2000.00"), Instant.now(), "Excédent caisse",
                null, null, BudgetMovementSource.INCONNUE, null, USER_ID, Instant.now()
        );

        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithEditor());
        when(budgetService.creditFromExternalSourceWithMovement(eq(BUDGET_ID), any(BudgetCreditCommand.class)))
                .thenReturn(movement);
        when(repository.save(any(Verification.class))).thenAnswer(inv -> inv.getArgument(0));

        Verification result = service.create(cmd);

        ArgumentCaptor<BudgetCreditCommand> creditCaptor = ArgumentCaptor.forClass(BudgetCreditCommand.class);
        verify(budgetService).creditFromExternalSourceWithMovement(eq(BUDGET_ID), creditCaptor.capture());
        assertThat(creditCaptor.getValue().amount()).isEqualByComparingTo("2000.00");
        assertThat(creditCaptor.getValue().reason()).isEqualTo("Excédent caisse");
        assertThat(result.regularizationKind()).isEqualTo(RegularizationKind.CREDIT);
        assertThat(result.regularizationCreditMovementId()).isEqualTo(movementId);
        assertThat(result.regularizationPurchaseId()).isNull();
        verify(purchaseService, never()).create(any());
    }

    @Test
    void create_rejectsMismatchedDenominationSum() {
        VerificationCreateCommand cmd = new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                null,
                new BigDecimal("12000.00"),
                new BigDecimal("12000.00"),
                List.of(new DenominationCount(10000, 1)),
                null
        );

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("ne correspond pas");
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsPurchaseModeWithNonNegativeDifference() {
        VerificationCreateCommand cmd = new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                List.of(new DenominationCount(10000, 1)),
                new RegularizationCommand(RegularizationKind.PURCHASE, "x", LocalDate.now())
        );
        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithEditor());

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("manquant");
    }

    @Test
    void create_rejectsCreditModeWithNonPositiveDifference() {
        VerificationCreateCommand cmd = new VerificationCreateCommand(
                BUDGET_ID,
                LocalDate.now(),
                null,
                new BigDecimal("10000.00"),
                new BigDecimal("10000.00"),
                List.of(new DenominationCount(10000, 1)),
                new RegularizationCommand(RegularizationKind.CREDIT, "x", LocalDate.now())
        );
        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithEditor());

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("excédent");
    }

    @Test
    void create_throwsForbidden_whenUserIsNotEditor() {
        UUID otherUser = UUID.randomUUID();
        Budget budgetWithoutEditor = new Budget(
                BUDGET_ID, "Caisse", "", new BigDecimal("100000.00"),
                otherUser, Set.of(otherUser), Instant.now(), null
        );
        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(budgetWithoutEditor);

        assertThatThrownBy(() -> service.create(commandBalanced()))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void search_appliesOwnerFilterWhenUserHasNoElevatedAuthority() {
        when(currentUserProvider.hasAuthority("PERM_verification:read:any")).thenReturn(false);
        when(currentUserProvider.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(USER_ID);
        when(repository.search(BUDGET_ID, null, null, USER_ID, 0, 10))
                .thenReturn(new PagedResponse<>(List.of(), 0, 10, 0, 0, true));

        service.search(BUDGET_ID, null, null, 0, 10);

        verify(repository).search(BUDGET_ID, null, null, USER_ID, 0, 10);
    }

    @Test
    void search_skipsOwnerFilterForSuperAdmin() {
        when(currentUserProvider.hasAuthority("PERM_verification:read:any")).thenReturn(false);
        when(currentUserProvider.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(true);
        when(repository.search(BUDGET_ID, null, null, null, 0, 10))
                .thenReturn(new PagedResponse<>(List.of(), 0, 10, 0, 0, true));

        service.search(BUDGET_ID, null, null, 0, 10);

        verify(repository).search(BUDGET_ID, null, null, null, 0, 10);
    }
}
