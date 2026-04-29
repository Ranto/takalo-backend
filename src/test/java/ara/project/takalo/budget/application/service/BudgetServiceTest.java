package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetTransferCommand;
import ara.project.takalo.budget.application.port.in.BudgetTransferResult;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository repository;

    @Mock
    private BudgetMovementRepository movementRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private BudgetService service;

    private Budget input(String name, BigDecimal fond) {
        return new Budget(null, name, "desc", fond, null, null, null, null);
    }

    private Budget budget(UUID id, BigDecimal fond, UUID editor) {
        return new Budget(id, "B-" + id, null, fond, editor,
                editor == null ? Set.of() : Set.of(editor), Instant.now(), null);
    }

    @Test
    void create_setsCurrentUserAsCreatorAndSoleEditor() {
        UUID currentUser = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.existsByName("Courses")).thenReturn(false);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = service.create(input("Courses", new BigDecimal("500.00")));

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(captor.capture());
        Budget passed = captor.getValue();
        assertThat(passed.createdBy()).isEqualTo(currentUser);
        assertThat(passed.editorIds()).containsExactly(currentUser);
        assertThat(passed.initialFund()).isEqualByComparingTo("500.00");
        assertThat(result.name()).isEqualTo("Courses");
    }

    @Test
    void create_acceptsNegativeAndZeroFunds() {
        UUID currentUser = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.existsByName(any())).thenReturn(false);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input("Negatif", new BigDecimal("-100.00")));
        service.create(input("Zero", BigDecimal.ZERO));

        verify(repository, times(2)).save(any(Budget.class));
    }

    @Test
    void create_whenNameAlreadyExists_throwsAlreadyExists() {
        when(repository.existsByName("Dup")).thenReturn(true);

        assertThatThrownBy(() -> service.create(input("Dup", new BigDecimal("10.00"))))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void getById_whenNoPurchases_resteEqualsFond() {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("500.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        when(repository.findById(id)).thenReturn(Optional.of(budget));
        when(repository.totalPurchasesByBudgetIds(List.of(id))).thenReturn(Map.of());

        BudgetWithBalance result = service.getById(id);

        assertThat(result.budget()).isSameAs(budget);
        assertThat(result.totalPurchases()).isEqualByComparingTo("0");
    }

    @Test
    void getById_whenPurchasesExceedFund_resteIsNegative() {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        when(repository.findById(id)).thenReturn(Optional.of(budget));
        when(repository.totalPurchasesByBudgetIds(List.of(id)))
                .thenReturn(Map.of(id, new BigDecimal("150.00")));

        BudgetWithBalance result = service.getById(id);

        assertThat(result.totalPurchases()).isEqualByComparingTo("150.00");
        assertThat(budget.initialFund().subtract(result.totalPurchases()))
                .isEqualByComparingTo("-50.00");
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget non trouvé");
    }

    @Test
    void findAll_computesBalanceForEachBudgetInOneCall() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Budget b1 = new Budget(id1, "A", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        Budget b2 = new Budget(id2, "B", null, new BigDecimal("200.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        PagedResponse<Budget> page = new PagedResponse<>(List.of(b1, b2), 0, 10, 2L, 1, true);
        when(repository.findAll(0, 10)).thenReturn(page);
        when(repository.totalPurchasesByBudgetIds(List.of(id1, id2)))
                .thenReturn(Map.of(id1, new BigDecimal("30.00")));

        PagedResponse<BudgetWithBalance> result = service.findAll(0, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).totalPurchases()).isEqualByComparingTo("30.00");
        assertThat(result.content().get(1).totalPurchases()).isEqualByComparingTo("0");
        verify(repository).totalPurchasesByBudgetIds(List.of(id1, id2));
    }

    @Test
    void findAll_emptyPage_skipsTotalsCall() {
        PagedResponse<Budget> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(repository.findAll(0, 10)).thenReturn(page);

        PagedResponse<BudgetWithBalance> result = service.findAll(0, 10);

        assertThat(result.content()).isEmpty();
        verify(repository, never()).totalPurchasesByBudgetIds(any());
    }

    // ------------------------------------------------------------------
    // Crédit externe (S30 → S35)
    // ------------------------------------------------------------------

    @Test
    void credit_increasesFund_andRecordsMovement() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Budget existing = budget(id, new BigDecimal("500.00"), alice);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.totalPurchasesByBudgetIds(List.of(id)))
                .thenReturn(Map.of(id, new BigDecimal("200.00")));

        Instant date = Instant.parse("2026-04-15T09:00:00Z");
        BudgetWithBalance result = service.creditFromExternalSource(id,
                new BudgetCreditCommand(new BigDecimal("200.00"), date, "Remboursement"));

        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(budgetCaptor.capture());
        assertThat(budgetCaptor.getValue().initialFund()).isEqualByComparingTo("700.00");

        ArgumentCaptor<BudgetMovement> mvCaptor = ArgumentCaptor.forClass(BudgetMovement.class);
        verify(movementRepository).save(mvCaptor.capture());
        BudgetMovement mv = mvCaptor.getValue();
        assertThat(mv.type()).isEqualTo(BudgetMovementType.CREDIT_EXTERNE);
        assertThat(mv.amount()).isEqualByComparingTo("200.00");
        assertThat(mv.source()).isEqualTo(BudgetMovementSource.INCONNUE);
        assertThat(mv.occurredAt()).isEqualTo(date);
        assertThat(mv.reason()).isEqualTo("Remboursement");
        assertThat(mv.counterpartBudgetId()).isNull();
        assertThat(mv.correlationId()).isNull();

        // S31 : reste = (fond + crédit) − Σ achats
        assertThat(result.budget().initialFund().subtract(result.totalPurchases()))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void credit_negativeFund_canTurnPositive() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Budget existing = budget(id, new BigDecimal("-200.00"), alice);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.totalPurchasesByBudgetIds(List.of(id))).thenReturn(Map.of());

        BudgetWithBalance result = service.creditFromExternalSource(id,
                new BudgetCreditCommand(new BigDecimal("300.00"),
                        Instant.parse("2026-04-15T09:00:00Z"), "Versement"));

        assertThat(result.budget().initialFund()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-50"})
    void credit_amountNotPositive_throws400(String amount) {
        UUID id = UUID.randomUUID();
        BudgetCreditCommand cmd = new BudgetCreditCommand(new BigDecimal(amount),
                Instant.now(), "raison");

        assertThatThrownBy(() -> service.creditFromExternalSource(id, cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Le montant du crédit doit être strictement positif");

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void credit_nonEditor_throws403() {
        UUID id = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Budget existing = budget(id, new BigDecimal("1000.00"), bob);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.creditFromExternalSource(id,
                new BudgetCreditCommand(new BigDecimal("100.00"), Instant.now(), "Tentative")))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void credit_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creditFromExternalSource(id,
                new BudgetCreditCommand(new BigDecimal("100.00"), Instant.now(), "raison")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // findMovements (S36)
    // ------------------------------------------------------------------

    @Test
    void findMovements_filtersByType() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);
        BudgetMovement c1 = new BudgetMovement(UUID.randomUUID(), id,
                BudgetMovementType.CREDIT_EXTERNE, new BigDecimal("100.00"),
                Instant.parse("2026-04-10T09:00:00Z"), "Cadeau",
                null, null, BudgetMovementSource.INCONNUE, null, null, null);
        BudgetMovement c2 = new BudgetMovement(UUID.randomUUID(), id,
                BudgetMovementType.CREDIT_EXTERNE, new BigDecimal("50.00"),
                Instant.parse("2026-04-20T09:00:00Z"), "Remboursement",
                null, null, BudgetMovementSource.INCONNUE, null, null, null);
        when(movementRepository.findByBudgetIdAndTypeOrderByOccurredAtAsc(id,
                BudgetMovementType.CREDIT_EXTERNE)).thenReturn(List.of(c1, c2));

        List<BudgetMovement> result = service.findMovements(id, BudgetMovementType.CREDIT_EXTERNE);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> m.source() == BudgetMovementSource.INCONNUE);
        BigDecimal sum = result.stream().map(BudgetMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("150.00");
        assertThat(result).allMatch(m -> m.occurredAt() != null && m.reason() != null);
    }

    @Test
    void findMovements_whenBudgetUnknown_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.findMovements(id, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Transfert (S21 → S29)
    // ------------------------------------------------------------------

    @Test
    void transfer_movesFunds_andRecordsMirrorMovements() {
        UUID alice = UUID.randomUUID();
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        Budget src = budget(srcId, new BigDecimal("500.00"), alice);
        Budget tgt = budget(tgtId, new BigDecimal("100.00"), alice);
        when(repository.findById(srcId)).thenReturn(Optional.of(src));
        when(repository.findById(tgtId)).thenReturn(Optional.of(tgt));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.totalPurchasesByBudgetIds(List.of(srcId)))
                .thenReturn(Map.of(srcId, new BigDecimal("200.00")));
        when(repository.totalPurchasesByBudgetIds(List.of(tgtId)))
                .thenReturn(Map.of(tgtId, new BigDecimal("25.00")));

        Instant date = Instant.parse("2026-04-29T10:00:00Z");
        BudgetTransferResult result = service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("150.00"), date, "Réallocation"));

        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(repository, times(2)).save(budgetCaptor.capture());
        List<Budget> saved = budgetCaptor.getAllValues();
        assertThat(saved.get(0).id()).isEqualTo(srcId);
        assertThat(saved.get(0).initialFund()).isEqualByComparingTo("350.00");
        assertThat(saved.get(1).id()).isEqualTo(tgtId);
        assertThat(saved.get(1).initialFund()).isEqualByComparingTo("250.00");

        ArgumentCaptor<BudgetMovement> mvCaptor = ArgumentCaptor.forClass(BudgetMovement.class);
        verify(movementRepository, times(2)).save(mvCaptor.capture());
        List<BudgetMovement> movements = mvCaptor.getAllValues();
        BudgetMovement out = movements.get(0);
        BudgetMovement in = movements.get(1);

        assertThat(out.type()).isEqualTo(BudgetMovementType.TRANSFERT_SORTANT);
        assertThat(out.amount()).isEqualByComparingTo("-150.00");
        assertThat(out.budgetId()).isEqualTo(srcId);
        assertThat(out.counterpartBudgetId()).isEqualTo(tgtId);

        assertThat(in.type()).isEqualTo(BudgetMovementType.TRANSFERT_ENTRANT);
        assertThat(in.amount()).isEqualByComparingTo("150.00");
        assertThat(in.budgetId()).isEqualTo(tgtId);
        assertThat(in.counterpartBudgetId()).isEqualTo(srcId);

        // Même correlationId pour les deux mouvements miroirs (S21 / futur S60).
        assertThat(out.correlationId()).isNotNull().isEqualTo(in.correlationId());
        assertThat(out.occurredAt()).isEqualTo(date);
        assertThat(in.occurredAt()).isEqualTo(date);
        assertThat(out.reason()).isEqualTo("Réallocation");
        assertThat(in.reason()).isEqualTo("Réallocation");

        // S22 : restes recalculés à partir des achats inchangés.
        assertThat(result.source().budget().initialFund()
                .subtract(result.source().totalPurchases())).isEqualByComparingTo("150.00");
        assertThat(result.target().budget().initialFund()
                .subtract(result.target().totalPurchases())).isEqualByComparingTo("225.00");
    }

    @Test
    void transfer_canMakeSourceFundNegative() {
        UUID alice = UUID.randomUUID();
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        when(repository.findById(srcId))
                .thenReturn(Optional.of(budget(srcId, new BigDecimal("100.00"), alice)));
        when(repository.findById(tgtId))
                .thenReturn(Optional.of(budget(tgtId, new BigDecimal("0.00"), alice)));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.totalPurchasesByBudgetIds(any())).thenReturn(Map.of());

        BudgetTransferResult result = service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("250.00"),
                Instant.parse("2026-04-29T10:00:00Z"), "Découvert"));

        assertThat(result.source().budget().initialFund()).isEqualByComparingTo("-150.00");
        assertThat(result.target().budget().initialFund()).isEqualByComparingTo("250.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-10"})
    void transfer_amountNotPositive_throws400(String amount) {
        BudgetTransferCommand cmd = new BudgetTransferCommand(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(amount),
                Instant.now(), "raison");

        assertThatThrownBy(() -> service.transfer(cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Le montant du transfert doit être strictement positif");

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void transfer_sameBudget_throws400() {
        UUID id = UUID.randomUUID();
        BudgetTransferCommand cmd = new BudgetTransferCommand(
                id, id, new BigDecimal("50.00"), Instant.now(), "raison");

        assertThatThrownBy(() -> service.transfer(cmd))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Le budget source et le budget cible doivent être différents");

        verify(repository, never()).save(any());
    }

    @Test
    void transfer_nonEditorOnSource_throws403() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        when(repository.findById(srcId))
                .thenReturn(Optional.of(budget(srcId, new BigDecimal("1000.00"), bob)));
        when(repository.findById(tgtId))
                .thenReturn(Optional.of(budget(tgtId, new BigDecimal("100.00"), alice)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("100.00"), Instant.now(), "raison")))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void transfer_nonEditorOnTarget_throws403() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        when(repository.findById(srcId))
                .thenReturn(Optional.of(budget(srcId, new BigDecimal("500.00"), alice)));
        when(repository.findById(tgtId))
                .thenReturn(Optional.of(budget(tgtId, new BigDecimal("1000.00"), bob)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("100.00"), Instant.now(), "raison")))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void transfer_unknownSource_throws404() {
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        when(repository.findById(srcId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("50.00"), Instant.now(), "raison")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Suppression (S37 / S38 — la garde 403 anticipe S40)
    // ------------------------------------------------------------------

    @Test
    void delete_existingBudget_callsRepository() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, BigDecimal.ZERO, alice)));
        when(currentUserProvider.id()).thenReturn(alice);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void delete_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget non trouvé");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_byNonEditor_throws403() {
        UUID id = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, BigDecimal.ZERO, bob)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).deleteById(any());
    }

    // ------------------------------------------------------------------
    // Restriction de modification aux éditeurs (S39 / S41)
    // ------------------------------------------------------------------

    @Test
    void update_byNonEditor_throwsForbidden_andDoesNotPersist() {
        UUID id = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, BigDecimal.ZERO, bob)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.update(id,
                new ara.project.takalo.budget.application.port.in.BudgetUpdateCommand(
                        "Vacances", null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void findMovements_byNonEditor_returnsMovements_whenBudgetExists() {
        UUID id = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);
        BudgetMovement credit = new BudgetMovement(UUID.randomUUID(), id,
                BudgetMovementType.CREDIT_EXTERNE, new BigDecimal("200.00"),
                Instant.parse("2026-04-15T09:00:00Z"), "Cadeau",
                null, null, BudgetMovementSource.INCONNUE, null, null, null);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(id))
                .thenReturn(List.of(credit));

        // alice n'est pas éditrice, mais findMovements ne consulte pas editorIds.
        // Le mock currentUserProvider n'est volontairement pas paramétré : aucun appel attendu.
        List<BudgetMovement> result = service.findMovements(id, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(BudgetMovementType.CREDIT_EXTERNE);
        assertThat(result.get(0).source()).isEqualTo(BudgetMovementSource.INCONNUE);
        // Sanity : on ne lookup pas le budget pour vérifier les éditeurs.
        verify(repository, never()).findById(any());
    }

    @Test
    void transfer_atomic_unknownTargetLeavesSourceUnchanged() {
        UUID alice = UUID.randomUUID();
        UUID srcId = UUID.randomUUID();
        UUID tgtId = UUID.randomUUID();
        when(repository.findById(srcId))
                .thenReturn(Optional.of(budget(srcId, new BigDecimal("500.00"), alice)));
        when(repository.findById(tgtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(new BudgetTransferCommand(
                srcId, tgtId, new BigDecimal("50.00"), Instant.now(), "raison")))
                .isInstanceOf(ResourceNotFoundException.class);

        // S29 : aucun save sur la source n'a eu lieu avant la découverte de la cible manquante.
        verify(repository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Mise à jour des méta-données (S13 / S14)
    // ------------------------------------------------------------------

    @Test
    void update_rejectsFondChange_throws400() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(id,
                new ara.project.takalo.budget.application.port.in.BudgetUpdateCommand(
                        "Courses", "desc", Optional.of(new BigDecimal("800.00")))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Le fond initial d'un budget ne peut pas être modifié, utiliser un crédit");

        // S13 : aucun lookup ni save : le contrôle se fait avant tout accès repo.
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void update_changesNameAndDescription_keepsFond() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        Budget existing = new Budget(id, "Courses", "desc",
                new BigDecimal("500.00"), alice, Set.of(alice), createdAt, null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.existsByName("Alimentation")).thenReturn(false);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.totalPurchasesByBudgetIds(List.of(id)))
                .thenReturn(Map.of(id, new BigDecimal("200.00")));

        BudgetWithBalance result = service.update(id,
                new ara.project.takalo.budget.application.port.in.BudgetUpdateCommand(
                        "Alimentation", "Courses hebdomadaires + extra", Optional.empty()));

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(captor.capture());
        Budget saved = captor.getValue();
        assertThat(saved.name()).isEqualTo("Alimentation");
        assertThat(saved.description()).isEqualTo("Courses hebdomadaires + extra");
        assertThat(saved.initialFund()).isEqualByComparingTo("500.00");
        assertThat(saved.createdBy()).isEqualTo(alice);
        assertThat(saved.createdAt()).isEqualTo(createdAt);
        assertThat(result.totalPurchases()).isEqualByComparingTo("200.00");
    }
}
