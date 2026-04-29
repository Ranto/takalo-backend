package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseLightResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseLightWebMapperTest {

    private final PurchaseLightWebMapper mapper = new PurchaseLightWebMapper();

    @Test
    void toResponse_setsItemCountAndTotalAmount() {
        UUID id = UUID.randomUUID();
        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        PurchaseItem item1 = new PurchaseItem(UUID.randomUUID(), 2.0, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, null, "A");
        PurchaseItem item2 = new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("5.00"),
                new BigDecimal("0.50"), null, null, "B");
        Purchase domain = new Purchase(id, null, null, date, List.of(item1, item2));

        PurchaseLightResponse response = mapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.purchaseDate()).isEqualTo(date);
        assertThat(response.itemCount()).isEqualTo(2);
        // 20 + 4.5 = 24.5
        assertThat(response.totalAmount()).isEqualByComparingTo("24.50");
    }
}
