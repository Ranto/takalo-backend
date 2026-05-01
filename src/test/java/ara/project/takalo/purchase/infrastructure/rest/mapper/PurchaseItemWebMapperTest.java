package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseItemWebMapperTest {

    private final PurchaseItemWebMapper mapper = new PurchaseItemWebMapper();

    @Test
    void toDomain_mapsProductNameAndLeavesProductIdNull() {
        PurchaseItemRequest request = new PurchaseItemRequest(
                "Lait",
                new BigDecimal("10.00"),
                2.0,
                new BigDecimal("1.00"),
                LocalDate.of(2030, 1, 1),
                "Carrefour"
        );

        PurchaseItem domain = mapper.toDomain(request);

        assertThat(domain.productId()).isNull();
        assertThat(domain.productName()).isEqualTo("Lait");
        assertThat(domain.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(domain.quantity()).isEqualTo(2.0);
        assertThat(domain.discount()).isEqualByComparingTo("1.00");
        assertThat(domain.expiryDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(domain.storeName()).isEqualTo("Carrefour");
    }

    @Test
    void toDomain_whenDiscountNull_defaultsToZero() {
        PurchaseItemRequest request = new PurchaseItemRequest(
                "Pain",
                new BigDecimal("10.00"),
                1.0,
                null,
                null,
                null
        );

        PurchaseItem domain = mapper.toDomain(request);

        assertThat(domain.discount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID productId = UUID.randomUUID();
        PurchaseItem domain = new PurchaseItem(
                productId,
                3.0,
                new BigDecimal("5.50"),
                new BigDecimal("0.50"),
                LocalDate.of(2030, 6, 1),
                "Auchan",
                "Pain"
        );

        PurchaseItemResponse response = mapper.toResponse(domain);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.quantity()).isEqualTo(3.0);
        assertThat(response.unitPrice()).isEqualByComparingTo("5.50");
        assertThat(response.discount()).isEqualByComparingTo("0.50");
        assertThat(response.expiryDate()).isEqualTo(LocalDate.of(2030, 6, 1));
        assertThat(response.storeName()).isEqualTo("Auchan");
        assertThat(response.productName()).isEqualTo("Pain");
    }
}
