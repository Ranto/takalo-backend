package ara.project.takalo.purchase.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository repository;

    @Mock
    private ProductServicePort productService;

    @InjectMocks
    private PurchaseService service;

    @Test
    void create_enrichesItemsWithProductNamesAndSaves() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        PurchaseItem item1 = new PurchaseItem(productId1, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        PurchaseItem item2 = new PurchaseItem(productId2, 2.0, new BigDecimal("4.00"), BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, Instant.parse("2024-01-01T00:00:00Z"), List.of(item1, item2));
        Purchase saved = new Purchase(UUID.randomUUID(), input.purchaseDate(), List.of());

        when(productService.getProductNames(Set.of(productId1, productId2)))
                .thenReturn(Map.of(productId1, "Lait", productId2, "Pain"));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.create(input);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        Purchase passed = captor.getValue();
        assertThat(passed.id()).isNull();
        assertThat(passed.purchaseDate()).isEqualTo(input.purchaseDate());
        assertThat(passed.items()).extracting(PurchaseItem::productName)
                .containsExactlyInAnyOrder("Lait", "Pain");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_whenProductNameMissing_usesFallback() {
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        Purchase input = new Purchase(null, Instant.now(), List.of(item));

        when(productService.getProductNames(Set.of(productId))).thenReturn(Map.of());
        when(repository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        Purchase result = service.create(input);

        assertThat(result.items().getFirst().productName()).isEqualTo("Produit supprimé");
    }

    @Test
    void update_whenFound_savesEnrichedPurchase() {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("3.00"), BigDecimal.ZERO, null, null, null);
        Purchase existing = new Purchase(id, Instant.parse("2024-01-01T00:00:00Z"), List.of());
        Purchase body = new Purchase(null, Instant.parse("2024-02-01T00:00:00Z"), List.of(item));
        Purchase saved = new Purchase(id, body.purchaseDate(), List.of());

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(productService.getProductNames(Set.of(productId))).thenReturn(Map.of(productId, "Lait"));
        when(repository.save(any(Purchase.class))).thenReturn(saved);

        Purchase result = service.update(id, body);

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().items().getFirst().productName()).isEqualTo("Lait");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        Purchase body = new Purchase(null, Instant.now(), List.of());

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
    void getById_whenFound_returnsDomain() {
        UUID id = UUID.randomUUID();
        Purchase found = new Purchase(id, Instant.now(), List.of());
        when(repository.findById(id)).thenReturn(Optional.of(found));

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
    void search_delegatesToRepository() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        PagedResponse<Purchase> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(repository.findByDateRange(start, end, 0, 10)).thenReturn(page);

        PagedResponse<Purchase> result = service.search(start, end, 0, 10);

        assertThat(result).isSameAs(page);
    }
}
