package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.domain.model.BudgetTimeline;
import ara.project.takalo.budget.domain.model.BudgetTimelineEvent;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.application.port.in.UserServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTimelineTest {

    @Mock
    private BudgetRepository repository;

    @Mock
    private BudgetMovementRepository movementRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserServicePort userService;

    @Mock
    private UserDefaultBudgetServicePort defaultBudgetService;

    @Mock
    private PurchaseServicePort purchaseService;

    @InjectMocks
    private BudgetService service;

    private Budget budget(UUID id, Instant createdAt, BigDecimal currentFund, UUID creator) {
        return new Budget(id, "Courses", null, currentFund, creator,
                Set.of(creator), createdAt, null);
    }

    private BudgetMovement movement(UUID budgetId, BudgetMovementType type, BigDecimal amount,
                                    String dateIso, String reason, UUID correlationId,
                                    UUID purchaseId, BudgetMovementSource source,
                                    UUID counterpart) {
        return new BudgetMovement(UUID.randomUUID(), budgetId, type, amount,
                Instant.parse(dateIso), reason, correlationId, purchaseId, source,
                counterpart, null, null);
    }

    private Purchase purchase(UUID id, UUID budgetId, String dateIso, String amount) {
        PurchaseItem item = new PurchaseItem(UUID.randomUUID(), 1.0,
                new BigDecimal(amount), null, null, null, "p");
        return new Purchase(id, UUID.randomUUID(), budgetId,
                Instant.parse(dateIso), null, null, null, List.of(item));
    }

    // ------------------------------------------------------------------
    // S56 - CREATION comme premier point
    // ------------------------------------------------------------------

    @Test
    void timeline_emptyBudget_returnsCreationEventOnly() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        when(repository.findById(id))
                .thenReturn(Optional.of(budget(id, createdAt, new BigDecimal("500.00"), alice)));
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(id)).thenReturn(List.of());
        when(purchaseService.findByBudgetId(id)).thenReturn(List.of());

        BudgetTimeline timeline = service.findTimeline(id, null, null);

        assertThat(timeline.events()).hasSize(1);
        BudgetTimelineEvent first = timeline.events().get(0);
        assertThat(first.type()).isEqualTo(BudgetMovementType.CREATION);
        assertThat(first.date()).isEqualTo(createdAt);
        assertThat(first.amount()).isEqualByComparingTo("500.00");
        assertThat(first.remainingBalance()).isEqualByComparingTo("500.00");
    }

    // ------------------------------------------------------------------
    // S57 - Agrégation chronologique de tous les évènements
    // ------------------------------------------------------------------

    @Test
    void timeline_aggregatesAllEventTypes_sortedByDate_withCumulativeBalance() {
        UUID id = UUID.randomUUID();
        UUID loisirs = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        // initialFund actuel = 500 (création) + 100 (crédit) - 50 (transfert sortant) = 550
        when(repository.findById(id))
                .thenReturn(Optional.of(budget(id, createdAt, new BigDecimal("550.00"), alice)));

        BudgetMovement credit = movement(id, BudgetMovementType.CREDIT_EXTERNE,
                new BigDecimal("100.00"), "2026-04-05T09:00:00Z", "Cadeau",
                null, null, BudgetMovementSource.INCONNUE, null);
        BudgetMovement transferOut = movement(id, BudgetMovementType.TRANSFERT_SORTANT,
                new BigDecimal("-50.00"), "2026-04-15T10:00:00Z", "Réajustement",
                UUID.randomUUID(), null, null, loisirs);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(id))
                .thenReturn(List.of(credit, transferOut));

        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();
        Purchase p1 = purchase(p1Id, id, "2026-04-10T12:00:00Z", "80.00");
        Purchase p2 = purchase(p2Id, id, "2026-04-20T12:00:00Z", "30.00");
        when(purchaseService.findByBudgetId(id)).thenReturn(List.of(p1, p2));

        BudgetTimeline timeline = service.findTimeline(id, null, null);

        assertThat(timeline.events()).hasSize(5);
        assertThat(timeline.events()).extracting(BudgetTimelineEvent::type)
                .containsExactly(
                        BudgetMovementType.CREATION,
                        BudgetMovementType.CREDIT_EXTERNE,
                        BudgetMovementType.ACHAT,
                        BudgetMovementType.TRANSFERT_SORTANT,
                        BudgetMovementType.ACHAT);
        assertThat(timeline.events()).extracting(e -> e.remainingBalance().setScale(2))
                .containsExactly(
                        new BigDecimal("500.00"),
                        new BigDecimal("600.00"),
                        new BigDecimal("520.00"),
                        new BigDecimal("470.00"),
                        new BigDecimal("440.00"));
        assertThat(timeline.events().get(2).amount()).isEqualByComparingTo("-80.00");
        assertThat(timeline.events().get(4).amount()).isEqualByComparingTo("-30.00");
    }

    // ------------------------------------------------------------------
    // S58 - Filtrage sur fenêtre [start, end] + restes de référence
    // ------------------------------------------------------------------

    @Test
    void timeline_filtersByWindow_andComputesReferenceBalances() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        when(repository.findById(id))
                .thenReturn(Optional.of(budget(id, createdAt, new BigDecimal("600.00"), alice)));

        BudgetMovement credit = movement(id, BudgetMovementType.CREDIT_EXTERNE,
                new BigDecimal("100.00"), "2026-04-05T09:00:00Z", "Cadeau",
                null, null, BudgetMovementSource.INCONNUE, null);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(id))
                .thenReturn(List.of(credit));

        Purchase p1 = purchase(UUID.randomUUID(), id, "2026-04-10T12:00:00Z", "80.00");
        Purchase p2 = purchase(UUID.randomUUID(), id, "2026-04-20T12:00:00Z", "30.00");
        Purchase p3 = purchase(UUID.randomUUID(), id, "2026-05-02T12:00:00Z", "40.00");
        when(purchaseService.findByBudgetId(id)).thenReturn(List.of(p1, p2, p3));

        Instant start = Instant.parse("2026-04-08T00:00:00Z");
        Instant end = Instant.parse("2026-04-30T23:59:59Z");
        BudgetTimeline timeline = service.findTimeline(id, start, end);

        assertThat(timeline.events()).hasSize(2);
        assertThat(timeline.events()).extracting(BudgetTimelineEvent::type)
                .containsExactly(BudgetMovementType.ACHAT, BudgetMovementType.ACHAT);
        assertThat(timeline.events().get(0).remainingBalance()).isEqualByComparingTo("520.00");
        assertThat(timeline.events().get(1).remainingBalance()).isEqualByComparingTo("490.00");
        // Restes de référence : avant start (CREATION + CREDIT) et après end (idem moins p3).
        assertThat(timeline.restRefStart()).isEqualByComparingTo("600.00");
        assertThat(timeline.restRefEnd()).isEqualByComparingTo("490.00");
    }

    @Test
    void timeline_invalidWindow_throws400() {
        UUID id = UUID.randomUUID();
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-04-01T00:00:00Z");

        assertThatThrownBy(() -> service.findTimeline(id, start, end))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("date de début");
    }

    // ------------------------------------------------------------------
    // S59 - Réassignation : DESASSIGNATION sur source, ASSIGNATION sur cible
    // ------------------------------------------------------------------

    @Test
    void timeline_purchaseReassignment_producesDesassignationOnSource() {
        UUID courses = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        when(repository.findById(courses))
                .thenReturn(Optional.of(budget(courses, createdAt, new BigDecimal("500.00"), alice)));

        UUID purchaseId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        // L'achat a d'abord existé puis a été désassigné : il n'est plus lié à "Courses".
        BudgetMovement desassign = movement(courses, BudgetMovementType.DESASSIGNATION_ACHAT,
                new BigDecimal("80.00"), "2026-04-25T15:00:00Z", "Mauvaise affectation",
                correlationId, purchaseId, null, null);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(courses))
                .thenReturn(List.of(desassign));

        // L'achat n'est plus lié à "Courses" mais reste référencé par le mouvement.
        when(purchaseService.findByBudgetId(courses)).thenReturn(List.of());
        Purchase original = purchase(purchaseId, UUID.randomUUID(),
                "2026-04-10T12:00:00Z", "80.00");
        when(purchaseService.findByIds(any())).thenReturn(List.of(original));

        BudgetTimeline timeline = service.findTimeline(courses, null, null);

        assertThat(timeline.events()).extracting(BudgetTimelineEvent::type)
                .containsExactly(
                        BudgetMovementType.CREATION,
                        BudgetMovementType.ACHAT,
                        BudgetMovementType.DESASSIGNATION_ACHAT);
        assertThat(timeline.events()).extracting(e -> e.remainingBalance().setScale(2))
                .containsExactly(
                        new BigDecimal("500.00"),
                        new BigDecimal("420.00"),
                        new BigDecimal("500.00"));
        assertThat(timeline.events().get(2).correlationId()).isEqualTo(correlationId);
        assertThat(timeline.events().get(2).purchaseId()).isEqualTo(purchaseId);
    }

    // ------------------------------------------------------------------
    // S60 - Évènements miroirs sur transfert (même correlationId)
    // ------------------------------------------------------------------

    @Test
    void timeline_transferMovements_carryCorrelationIdAndCounterpart() {
        UUID courses = UUID.randomUUID();
        UUID loisirs = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        when(repository.findById(courses))
                .thenReturn(Optional.of(budget(courses, createdAt, new BigDecimal("350.00"), alice)));

        UUID correlationId = UUID.randomUUID();
        BudgetMovement out = movement(courses, BudgetMovementType.TRANSFERT_SORTANT,
                new BigDecimal("-150.00"), "2026-04-15T10:00:00Z", "Réallocation",
                correlationId, null, null, loisirs);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(courses))
                .thenReturn(List.of(out));
        when(purchaseService.findByBudgetId(courses)).thenReturn(List.of());

        BudgetTimeline timeline = service.findTimeline(courses, null, null);

        BudgetTimelineEvent transferEvent = timeline.events().stream()
                .filter(e -> e.type() == BudgetMovementType.TRANSFERT_SORTANT)
                .findFirst().orElseThrow();
        assertThat(transferEvent.amount()).isEqualByComparingTo("-150.00");
        assertThat(transferEvent.correlationId()).isEqualTo(correlationId);
        assertThat(transferEvent.counterpartBudgetId()).isEqualTo(loisirs);
        assertThat(transferEvent.reason()).isEqualTo("Réallocation");
    }

    // ------------------------------------------------------------------
    // S61 - Lecture timeline ouverte à tout authentifié (non-éditeur OK)
    // ------------------------------------------------------------------

    @Test
    void timeline_byNonEditor_isAllowed() {
        UUID id = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        // Budget créé par bob ; alice n'est pas éditrice mais peut consulter.
        when(repository.findById(id))
                .thenReturn(Optional.of(budget(id, createdAt, new BigDecimal("1000.00"), bob)));
        BudgetMovement credit = movement(id, BudgetMovementType.CREDIT_EXTERNE,
                new BigDecimal("200.00"), "2026-04-15T09:00:00Z", "Bonus",
                null, null, BudgetMovementSource.INCONNUE, null);
        when(movementRepository.findByBudgetIdOrderByOccurredAtAsc(id))
                .thenReturn(List.of(credit));
        when(purchaseService.findByBudgetId(id)).thenReturn(List.of());
        // Pas de stub sur currentUserProvider : findTimeline n'a pas à l'appeler.

        BudgetTimeline timeline = service.findTimeline(id, null, null);

        assertThat(timeline.events()).extracting(BudgetTimelineEvent::type)
                .containsExactly(BudgetMovementType.CREATION, BudgetMovementType.CREDIT_EXTERNE);
    }

    @Test
    void timeline_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTimeline(id, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
