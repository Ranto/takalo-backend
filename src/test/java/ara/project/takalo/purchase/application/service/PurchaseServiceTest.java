package ara.project.takalo.purchase.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.purchase.application.port.in.BulkReassignBudgetResult;
import ara.project.takalo.purchase.application.port.in.PurchaseItemDetailQuery;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository repository;

    @Mock
    private ProductServicePort productService;

    @Mock
    private BudgetServicePort budgetService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private PurchaseService service;

    @Test
    void create_resolvesItemsByProductNameAndSaves() {
        UUID currentUser = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        PurchaseItem item1 = new PurchaseItem(null, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, "Lait");
        PurchaseItem item2 = new PurchaseItem(null, 2.0, new BigDecimal("4.00"), BigDecimal.ZERO, null, null, "Pain");
        Purchase input = new Purchase(null, null, null, Instant.parse("2024-01-01T00:00:00Z"), null, null, null, List.of(item1, item2));
        Purchase saved = new Purchase(UUID.randomUUID(), currentUser, null, input.purchaseDate(), null, null, null, List.of());

        when(currentUserProvider.id()).thenReturn(currentUser);
        when(productService.findOrCreateByName("Lait"))
                .thenReturn(new Product(productId1, "Lait", null, null, null));
        when(productService.findOrCreateByName("Pain"))
                .thenReturn(new Product(productId2, "Pain", null, null, null));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.create(input);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        Purchase passed = captor.getValue();
        assertThat(passed.id()).isNull();
        assertThat(passed.ownerId()).isEqualTo(currentUser);
        assertThat(passed.purchaseDate()).isEqualTo(input.purchaseDate());
        assertThat(passed.items()).extracting(PurchaseItem::productId)
                .containsExactly(productId1, productId2);
        assertThat(passed.items()).extracting(PurchaseItem::productName)
                .containsExactly("Lait", "Pain");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_usesCanonicalProductNameWhenCaseDiffers() {
        UUID currentUser = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(null, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, "PÂTES");
        Purchase input = new Purchase(null, null, null, Instant.now(), null, null, null, List.of(item));

        when(currentUserProvider.id()).thenReturn(currentUser);
        when(productService.findOrCreateByName("PÂTES"))
                .thenReturn(new Product(existingId, "Pâtes complètes", null, null, null));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Purchase result = service.create(input);

        assertThat(result.items().getFirst().productId()).isEqualTo(existingId);
        assertThat(result.items().getFirst().productName()).isEqualTo("Pâtes complètes");
    }

    @Test
    void update_whenFound_savesResolvedPurchase() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(null, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, "Lait");
        Purchase existing = new Purchase(id, owner, null, Instant.parse("2024-01-01T00:00:00Z"), null, null, null, List.of());
        Purchase body = new Purchase(null, null, null, Instant.parse("2024-02-01T00:00:00Z"), null, null, null, List.of(item));
        Purchase saved = new Purchase(id, owner, null, body.purchaseDate(), null, null, null, List.of());

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(productService.findOrCreateByName("Lait"))
                .thenReturn(new Product(productId, "Lait", null, null, null));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.update(id, body);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id);
        assertThat(captor.getValue().ownerId()).isEqualTo(owner);
        assertThat(captor.getValue().items().getFirst().productId()).isEqualTo(productId);
        assertThat(captor.getValue().items().getFirst().productName()).isEqualTo("Lait");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        Purchase body = new Purchase(null, null, null, Instant.now(), null, null, null, List.of());

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, body))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Achat non trouvé");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_whenOwner_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        Purchase existing = new Purchase(id, owner, null, Instant.now(), null, null, null, List.of());
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(owner);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void getById_whenOwnerWithReadOwn_returnsDomain() {
        UUID id = UUID.randomUUID();
        UUID currentUser = UUID.randomUUID();
        Purchase found = new Purchase(id, currentUser, null, Instant.now(), null, null, null, List.of());
        when(repository.findById(id)).thenReturn(Optional.of(found));
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(currentUser);

        Purchase result = service.getById(id);

        assertThat(result).isSameAs(found);
    }

    @Test
    void getById_whenAnotherUserWithReadOwnOnly_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Purchase found = new Purchase(id, owner, null, Instant.now(), null, null, null, List.of());
        when(repository.findById(id)).thenReturn(Optional.of(found));
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(other);

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_whenReadAny_returnsRegardlessOfOwner() {
        UUID id = UUID.randomUUID();
        Purchase found = new Purchase(id, UUID.randomUUID(), null, Instant.now(), null, null, null, List.of());
        when(repository.findById(id)).thenReturn(Optional.of(found));
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(true);

        Purchase result = service.getById(id);

        assertThat(result).isSameAs(found);
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Achat non trouvé");
    }

    // ------------------------------------------------------------------
    // Association achat ↔ budget (S15, S16, S17, S18, S20)
    // ------------------------------------------------------------------

    private Budget budgetWithEditors(UUID id, UUID... editors) {
        return new Budget(id, "B", null, BigDecimal.ZERO,
                editors[0], Set.of(editors), java.time.Instant.now(), null);
    }

    @Test
    void create_withBudget_byEditor_savesWithBudgetIdAndOwner() {
        UUID alice = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(null, 1.0, new BigDecimal("75.00"),
                BigDecimal.ZERO, null, null, "Pain");
        Purchase input = new Purchase(null, null, budgetId,
                Instant.parse("2026-04-10T12:00:00Z"), null, null, null, List.of(item));

        when(currentUserProvider.id()).thenReturn(alice);
        when(budgetService.getRawById(budgetId)).thenReturn(budgetWithEditors(budgetId, alice));
        when(productService.findOrCreateByName("Pain"))
                .thenReturn(new Product(productId, "Pain", null, null, null));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Purchase result = service.create(input);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo(alice);
        assertThat(captor.getValue().budgetId()).isEqualTo(budgetId);
        assertThat(result.budgetId()).isEqualTo(budgetId);
    }

    @Test
    void create_withBudget_byNonEditor_throws403() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("50.00"),
                BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, null, budgetId, Instant.now(), null, null, null, List.of(item));

        when(currentUserProvider.id()).thenReturn(alice);
        // Le budget est créé par bob, alice n'est pas dans la liste des éditeurs.
        when(budgetService.getRawById(budgetId)).thenReturn(budgetWithEditors(budgetId, bob));

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("éditeur");

        verify(repository, never()).save(any());
    }

    @Test
    void create_withUnknownBudget_throws404() {
        UUID alice = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("50.00"),
                BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, null, budgetId, Instant.now(), null, null, null, List.of(item));

        when(currentUserProvider.id()).thenReturn(alice);
        when(budgetService.getRawById(budgetId))
                .thenThrow(new ResourceNotFoundException("Budget non trouvé"));

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_withoutBudget_skipsEditorCheck() {
        UUID alice = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(null, 1.0, new BigDecimal("30.00"),
                BigDecimal.ZERO, null, null, "x");
        Purchase input = new Purchase(null, null, null, Instant.now(), null, null, null, List.of(item));

        when(currentUserProvider.id()).thenReturn(alice);
        when(productService.findOrCreateByName("x"))
                .thenReturn(new Product(productId, "x", null, null, null));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input);

        verify(budgetService, never()).getRawById(any());
    }

    @Test
    void delete_purchaseLinkedToBudget_deletesWithoutTouchingBudget() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Purchase existing = new Purchase(id, owner, budgetId, Instant.now(), null, null, null, List.of());
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(owner);

        service.delete(id);

        // S20 : la suppression ne touche pas au budget ; le reste est libéré du seul fait
        // que l'achat n'apparaît plus dans totalPurchasesByBudgetIds.
        verify(repository).deleteById(id);
        verify(budgetService, never()).recordPurchaseUnassignment(any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Réassignation d'achat (S19)
    // ------------------------------------------------------------------

    @Test
    void reassignBudget_betweenTwoBudgets_logsMirrorMovementsWithSameCorrelationId() {
        UUID alice = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        UUID courses = UUID.randomUUID();
        UUID loisirs = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("80.00"),
                BigDecimal.ZERO, null, null, "p");
        Purchase existing = new Purchase(purchaseId, alice, courses,
                Instant.parse("2026-04-10T12:00:00Z"), null, null, null, List.of(item));
        Instant when = Instant.parse("2026-04-29T10:00:00Z");

        when(repository.findById(purchaseId)).thenReturn(Optional.of(existing));
        when(currentUserProvider.id()).thenReturn(alice);
        when(budgetService.getRawById(courses)).thenReturn(budgetWithEditors(courses, alice));
        when(budgetService.getRawById(loisirs)).thenReturn(budgetWithEditors(loisirs, alice));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Purchase result = service.reassignBudget(purchaseId, loisirs, when, "Mauvaise affectation");

        assertThat(result.budgetId()).isEqualTo(loisirs);

        ArgumentCaptor<UUID> corrUnassign = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> corrAssign = ArgumentCaptor.forClass(UUID.class);
        verify(budgetService).recordPurchaseUnassignment(
                org.mockito.ArgumentMatchers.eq(courses),
                org.mockito.ArgumentMatchers.eq(purchaseId),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("80.000")),
                org.mockito.ArgumentMatchers.eq(when),
                org.mockito.ArgumentMatchers.eq("Mauvaise affectation"),
                corrUnassign.capture());
        verify(budgetService).recordPurchaseAssignment(
                org.mockito.ArgumentMatchers.eq(loisirs),
                org.mockito.ArgumentMatchers.eq(purchaseId),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("80.000")),
                org.mockito.ArgumentMatchers.eq(when),
                org.mockito.ArgumentMatchers.eq("Mauvaise affectation"),
                corrAssign.capture());
        // Les deux mouvements partagent le même correlationId.
        assertThat(corrUnassign.getValue()).isEqualTo(corrAssign.getValue());
    }

    // ------------------------------------------------------------------
    // Réassignation en masse (bulk)
    // ------------------------------------------------------------------

    @Test
    void reassignBudgetBulk_allFromSameBudget_appliesWithSharedCorrelationId() {
        UUID alice = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID courses = UUID.randomUUID();
        UUID loisirs = UUID.randomUUID();
        Instant when = Instant.parse("2026-04-29T10:00:00Z");
        Purchase a1 = new Purchase(p1, alice, courses, Instant.parse("2026-04-10T12:00:00Z"), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("80.00"),
                        BigDecimal.ZERO, null, null, "x")));
        Purchase a2 = new Purchase(p2, alice, courses, Instant.parse("2026-04-11T12:00:00Z"), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("20.00"),
                        BigDecimal.ZERO, null, null, "y")));

        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.findByIds(any())).thenReturn(List.of(a1, a2));
        when(budgetService.getRawById(courses)).thenReturn(budgetWithEditors(courses, alice));
        when(budgetService.getRawById(loisirs)).thenReturn(budgetWithEditors(loisirs, alice));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        BulkReassignBudgetResult result = service.reassignBudgetBulk(
                List.of(p1, p2), loisirs, when, "Mauvaise affectation");

        assertThat(result.purchases()).extracting(Purchase::budgetId).containsOnly(loisirs);
        assertThat(result.purchases()).extracting(Purchase::id).containsExactly(p1, p2);

        ArgumentCaptor<UUID> corr = ArgumentCaptor.forClass(UUID.class);
        verify(budgetService, org.mockito.Mockito.times(2)).recordPurchaseUnassignment(
                org.mockito.ArgumentMatchers.eq(courses), any(), any(),
                org.mockito.ArgumentMatchers.eq(when),
                org.mockito.ArgumentMatchers.eq("Mauvaise affectation"),
                corr.capture());
        verify(budgetService, org.mockito.Mockito.times(2)).recordPurchaseAssignment(
                org.mockito.ArgumentMatchers.eq(loisirs), any(), any(),
                org.mockito.ArgumentMatchers.eq(when),
                org.mockito.ArgumentMatchers.eq("Mauvaise affectation"),
                corr.capture());
        // Un seul correlationId partagé pour les 4 mouvements (2 unassign + 2 assign).
        assertThat(corr.getAllValues()).hasSize(4);
        assertThat(Set.copyOf(corr.getAllValues())).hasSize(1);
        assertThat(result.correlationId()).isEqualTo(corr.getValue());
    }

    @Test
    void reassignBudgetBulk_validatesEachOldBudgetOnceAndNewBudgetOnce() {
        UUID alice = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        UUID courses = UUID.randomUUID();
        UUID loisirs = UUID.randomUUID();
        UUID cible = UUID.randomUUID();
        Purchase a1 = new Purchase(p1, alice, courses, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "x")));
        Purchase a2 = new Purchase(p2, alice, courses, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "y")));
        Purchase a3 = new Purchase(p3, alice, loisirs, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "z")));

        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.findByIds(any())).thenReturn(List.of(a1, a2, a3));
        when(budgetService.getRawById(any())).thenAnswer(inv ->
                budgetWithEditors(inv.getArgument(0), alice));
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reassignBudgetBulk(List.of(p1, p2, p3), cible, Instant.now(), "r");

        // Chaque budget distinct vérifié une seule fois (courses, loisirs, cible).
        verify(budgetService).getRawById(courses);
        verify(budgetService).getRawById(loisirs);
        verify(budgetService).getRawById(cible);
    }

    @Test
    void reassignBudgetBulk_whenSomeIdsMissing_throws404AndDoesNotPersist() {
        UUID alice = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UUID cible = UUID.randomUUID();
        Purchase a1 = new Purchase(p1, alice, null, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "x")));

        when(repository.findByIds(any())).thenReturn(List.of(a1));

        assertThatThrownBy(() -> service.reassignBudgetBulk(
                List.of(p1, missing), cible, Instant.now(), "r"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missing.toString());

        verify(repository, never()).save(any());
        verify(budgetService, never()).recordPurchaseAssignment(any(), any(), any(), any(), any(), any());
        verify(budgetService, never()).recordPurchaseUnassignment(any(), any(), any(), any(), any(), any());
    }

    @Test
    void reassignBudgetBulk_whenNotOwnerOfSomePurchase_throws403() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID cible = UUID.randomUUID();
        Purchase mine = new Purchase(p1, alice, null, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "x")));
        Purchase others = new Purchase(p2, bob, null, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "y")));

        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.findByIds(any())).thenReturn(List.of(mine, others));

        assertThatThrownBy(() -> service.reassignBudgetBulk(
                List.of(p1, p2), cible, Instant.now(), "r"))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void reassignBudgetBulk_whenNotEditorOfTargetBudget_throws403() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID cible = UUID.randomUUID();
        Purchase a1 = new Purchase(p1, alice, null, Instant.now(), null, null, null,
                List.of(new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                        BigDecimal.ZERO, null, null, "x")));

        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.findByIds(any())).thenReturn(List.of(a1));
        when(budgetService.getRawById(cible)).thenReturn(budgetWithEditors(cible, bob));

        assertThatThrownBy(() -> service.reassignBudgetBulk(
                List.of(p1), cible, Instant.now(), "r"))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void search_withReadAny_delegatesToFindByDateRange() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        PagedResponse<Purchase> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(true);
        when(repository.findByDateRange(start, end, null, null, false, 0, 10)).thenReturn(page);

        PagedResponse<Purchase> result = service.search(start, end, null, null, false, 0, 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    void searchItemDetails_withReadAny_passesNullOwner() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, null,
                null, false, null, null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);
        PagedResponse<PurchaseItemDetail> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(true);
        when(repository.searchItemDetails(q, null)).thenReturn(page);

        PagedResponse<PurchaseItemDetail> result = service.searchItemDetails(q);

        assertThat(result).isSameAs(page);
    }

    @Test
    void searchItemDetails_withReadOwnOnly_filtersByCurrentUser() {
        UUID currentUser = UUID.randomUUID();
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, "lait", "produits laitiers",
                null, false, null, null,
                PurchaseItemDetailQuery.SortField.PRODUCT,
                PurchaseItemDetailQuery.SortDirection.ASC,
                0, 10);
        PagedResponse<PurchaseItemDetail> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.searchItemDetails(q, currentUser)).thenReturn(page);

        PagedResponse<PurchaseItemDetail> result = service.searchItemDetails(q);

        assertThat(result).isSameAs(page);
    }

    @Test
    void search_withReadOwnOnly_filtersByCurrentUser() {
        UUID currentUser = UUID.randomUUID();
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        PagedResponse<Purchase> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.findByDateRangeAndOwner(start, end, currentUser, null, null, false, 0, 10)).thenReturn(page);

        PagedResponse<Purchase> result = service.search(start, end, null, null, false, 0, 10);

        assertThat(result).isSameAs(page);
    }
}
