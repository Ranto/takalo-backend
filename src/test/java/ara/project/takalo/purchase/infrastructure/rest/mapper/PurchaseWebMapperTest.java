package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseWebMapperTest {

    private final PurchaseWebMapper mapper = new PurchaseWebMapper(new PurchaseItemWebMapper());

    @Test
    void toDomain_buildsDomainWithNullIdAndMappedItems() {
        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        UUID productId = UUID.randomUUID();
        PurchaseItemRequest itemRequest = new PurchaseItemRequest(
                productId,
                new BigDecimal("10.00"),
                2.0,
                new BigDecimal("1.00"),
                null,
                null,
                null
        );
        PurchaseRequest request = new PurchaseRequest(date, null, List.of(itemRequest));

        Purchase domain = mapper.toDomain(request);

        assertThat(domain.id()).isNull();
        assertThat(domain.purchaseDate()).isEqualTo(date);
        assertThat(domain.items()).hasSize(1);
        assertThat(domain.items().getFirst().productId()).isEqualTo(productId);
    }

    @Test
    void toResponse_includesItemsAndTotalAmount() {
        UUID id = UUID.randomUUID();
        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        PurchaseItem item1 = new PurchaseItem(UUID.randomUUID(), 2.0, new BigDecimal("10.00"),
                new BigDecimal("1.00"), null, null, "A");
        PurchaseItem item2 = new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("4.00"),
                BigDecimal.ZERO, null, null, "B");
        Purchase domain = new Purchase(id, null, null, date, List.of(item1, item2));

        PurchaseResponse response = mapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.purchaseDate()).isEqualTo(date);
        assertThat(response.items()).hasSize(2);
        // (2*10 - 1) + (1*4 - 0) = 19 + 4 = 23
        assertThat(response.totalAmount()).isEqualByComparingTo("23.00");
    }
}
