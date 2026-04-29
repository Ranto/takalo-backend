package ara.project.takalo.purchase.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
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
import java.util.Map;
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
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private PurchaseService service;

    @Test
    void create_enrichesItemsWithProductNamesAndSaves() {
        UUID currentUser = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        PurchaseItem item1 = new PurchaseItem(productId1, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        PurchaseItem item2 = new PurchaseItem(productId2, 2.0, new BigDecimal("4.00"), BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, null, Instant.parse("2024-01-01T00:00:00Z"), List.of(item1, item2));
        Purchase saved = new Purchase(UUID.randomUUID(), currentUser, input.purchaseDate(), List.of());

        when(currentUserProvider.id()).thenReturn(currentUser);
        when(productService.getProductNames(Set.of(productId1, productId2)))
                .thenReturn(Map.of(productId1, "Lait", productId2, "Pain"));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.create(input);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        Purchase passed = captor.getValue();
        assertThat(passed.id()).isNull();
        assertThat(passed.ownerId()).isEqualTo(currentUser);
        assertThat(passed.purchaseDate()).isEqualTo(input.purchaseDate());
        assertThat(passed.items()).extracting(PurchaseItem::productName)
                .containsExactlyInAnyOrder("Lait", "Pain");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_whenProductNameMissing_usesFallback() {
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, null, Instant.now(), List.of(item));

        when(currentUserProvider.id()).thenReturn(UUID.randomUUID());
        when(productService.getProductNames(Set.of(productId))).thenReturn(Map.of());
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Purchase result = service.create(input);

        assertThat(result.items().getFirst().productName()).isEqualTo("Produit supprimé");
    }

    @Test
    void update_whenFound_savesEnrichedPurchase() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        Purchase existing = new Purchase(id, owner, Instant.parse("2024-01-01T00:00:00Z"), List.of());
        Purchase body = new Purchase(null, null, Instant.parse("2024-02-01T00:00:00Z"), List.of(item));
        Purchase saved = new Purchase(id, owner, body.purchaseDate(), List.of());

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(productService.getProductNames(Set.of(productId))).thenReturn(Map.of(productId, "Lait"));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.update(id, body);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().ownerId()).isEqualTo(owner);
        assertThat(captor.getValue().items().getFirst().productName()).isEqualTo("Lait");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        Purchase body = new Purchase(null, null, Instant.now(), List.of());

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, body))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Achat non trouvé");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void getById_whenOwnerWithReadOwn_returnsDomain() {
        UUID id = UUID.randomUUID();
        UUID currentUser = UUID.randomUUID();
        Purchase found = new Purchase(id, currentUser, Instant.now(), List.of());
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
        Purchase found = new Purchase(id, owner, Instant.now(), List.of());
        when(repository.findById(id)).thenReturn(Optional.of(found));
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(false);
        when(currentUserProvider.id()).thenReturn(other);

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_whenReadAny_returnsRegardlessOfOwner() {
        UUID id = UUID.randomUUID();
        Purchase found = new Purchase(id, UUID.randomUUID(), Instant.now(), List.of());
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

    @Test
    void search_withReadAny_delegatesToFindByDateRange() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        PagedResponse<Purchase> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(currentUserProvider.hasAuthority("PERM_purchase:read:any")).thenReturn(true);
        when(repository.findByDateRange(start, end, 0, 10)).thenReturn(page);

        PagedResponse<Purchase> result = service.search(start, end, 0, 10);

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
        when(repository.findByDateRangeAndOwner(start, end, currentUser, 0, 10)).thenReturn(page);

        PagedResponse<Purchase> result = service.search(start, end, 0, 10);

        assertThat(result).isSameAs(page);
    }
}
