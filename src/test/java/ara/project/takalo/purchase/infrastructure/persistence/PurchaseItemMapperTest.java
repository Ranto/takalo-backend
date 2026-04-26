package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseItemEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseItemMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseItemMapperTest {

    private final PurchaseItemMapper mapper = new PurchaseItemMapper();

    @Test
    void toDomain_mapsAllFields() {
        UUID productId = UUID.randomUUID();
        PurchaseItemEntity entity = PurchaseItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(2.0)
                .unitPrice(new BigDecimal("10.00"))
                .discount(new BigDecimal("1.00"))
                .expiryDate(LocalDate.of(2030, 1, 1))
                .storeName("Carrefour")
                .productName("Lait")
                .build();

        PurchaseItem domain = mapper.toDomain(entity);

        assertThat(domain.productId()).isEqualTo(productId);
        assertThat(domain.quantity()).isEqualTo(2.0);
        assertThat(domain.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(domain.discount()).isEqualByComparingTo("1.00");
        assertThat(domain.expiryDate()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(domain.storeName()).isEqualTo("Carrefour");
        assertThat(domain.productName()).isEqualTo("Lait");
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsAllFields() {
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

        PurchaseItemEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getQuantity()).isEqualTo(3.0);
        assertThat(entity.getUnitPrice()).isEqualByComparingTo("5.50");
        assertThat(entity.getDiscount()).isEqualByComparingTo("0.50");
        assertThat(entity.getExpiryDate()).isEqualTo(LocalDate.of(2030, 6, 1));
        assertThat(entity.getStoreName()).isEqualTo("Auchan");
        assertThat(entity.getProductName()).isEqualTo("Pain");
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
