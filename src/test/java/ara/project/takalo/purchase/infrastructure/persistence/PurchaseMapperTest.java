package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseItemEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseItemMapper;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseMapperTest {

    private final PurchaseMapper mapper = new PurchaseMapper(new PurchaseItemMapper());

    @Test
    void toDomain_mapsFieldsAndItems() {
        UUID purchaseId = UUID.randomUUID();
        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        UUID productId = UUID.randomUUID();
        PurchaseItemEntity itemEntity = PurchaseItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(1.0)
                .unitPrice(new BigDecimal("4.00"))
                .discount(BigDecimal.ZERO)
                .productName("Lait")
                .build();
        List<PurchaseItemEntity> items = new ArrayList<>();
        items.add(itemEntity);

        PurchaseEntity entity = PurchaseEntity.builder()
                .id(purchaseId)
                .purchaseDate(date)
                .items(items)
                .build();

        Purchase domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(purchaseId);
        assertThat(domain.purchaseDate()).isEqualTo(date);
        assertThat(domain.items()).hasSize(1);
        PurchaseItem item = domain.items().getFirst();
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.productName()).isEqualTo("Lait");
    }

    @Test
    void toDomain_withMultipleItems_mapsAllLines() {
        UUID purchaseId = UUID.randomUUID();
        Instant date = Instant.parse("2026-04-20T10:00:00Z");

        PurchaseItemEntity milk = PurchaseItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2.0)
                .unitPrice(new BigDecimal("2.10"))
                .discount(new BigDecimal("0.30"))
                .productName("Lait")
                .storeName("Leclerc")
                .build();
        PurchaseItemEntity bread = PurchaseItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(1.0)
                .unitPrice(new BigDecimal("1.50"))
                .discount(BigDecimal.ZERO)
                .productName("Pain")
                .storeName("Leclerc")
                .build();
        PurchaseItemEntity yogurt = PurchaseItemEntity.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(4.0)
                .unitPrice(new BigDecimal("1.20"))
                .discount(BigDecimal.ZERO)
                .productName("Yaourt")
                .storeName("Leclerc")
                .build();
        List<PurchaseItemEntity> items = new ArrayList<>();
        items.add(milk);
        items.add(bread);
        items.add(yogurt);

        PurchaseEntity entity = PurchaseEntity.builder()
                .id(purchaseId)
                .purchaseDate(date)
                .items(items)
                .build();

        Purchase domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(purchaseId);
        assertThat(domain.purchaseDate()).isEqualTo(date);
        assertThat(domain.items()).hasSize(3);
        assertThat(domain.items())
                .extracting(PurchaseItem::productName)
                .containsExactlyInAnyOrder("Lait", "Pain", "Yaourt");
        assertThat(domain.items())
                .extracting(PurchaseItem::productId)
                .containsExactlyInAnyOrder(milk.getProductId(), bread.getProductId(), yogurt.getProductId());
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsFieldsAndAddsItemsWithBackReference() {
        UUID purchaseId = UUID.randomUUID();
        Instant date = Instant.parse("2024-02-01T00:00:00Z");
        PurchaseItem item = new PurchaseItem(
                UUID.randomUUID(),
                2.0,
                new BigDecimal("3.50"),
                BigDecimal.ZERO,
                null,
                "Carrefour",
                "Pain"
        );
        Purchase domain = new Purchase(purchaseId, null, null, date, null, null, null, List.of(item));

        PurchaseEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(purchaseId);
        assertThat(entity.getPurchaseDate()).isEqualTo(date);
        assertThat(entity.getItems()).hasSize(1);
        PurchaseItemEntity persisted = entity.getItems().iterator().next();
        assertThat(persisted.getPurchase()).isSameAs(entity);
        assertThat(persisted.getProductName()).isEqualTo("Pain");
    }

    @Test
    void toEntity_withMultipleNewItems_keepsAllLinesAndSetsBackReference() {
        PurchaseItem milk = new PurchaseItem(
                UUID.randomUUID(), 2.0, new BigDecimal("2.10"), new BigDecimal("0.30"),
                null, "Leclerc", "Lait");
        PurchaseItem bread = new PurchaseItem(
                UUID.randomUUID(), 1.0, new BigDecimal("1.50"), BigDecimal.ZERO,
                null, "Leclerc", "Pain");
        PurchaseItem yogurt = new PurchaseItem(
                UUID.randomUUID(), 4.0, new BigDecimal("1.20"), BigDecimal.ZERO,
                null, "Leclerc", "Yaourt");
        Purchase domain = new Purchase(UUID.randomUUID(), null, null,
                Instant.parse("2026-04-20T10:00:00Z"), null, null, null, List.of(milk, bread, yogurt));

        PurchaseEntity entity = mapper.toEntity(domain);

        assertThat(entity.getItems()).hasSize(3);
        assertThat(entity.getItems())
                .extracting(PurchaseItemEntity::getProductName)
                .containsExactlyInAnyOrder("Lait", "Pain", "Yaourt");
        assertThat(entity.getItems())
                .allMatch(i -> i.getPurchase() == entity);
    }

    @Test
    void toEntity_withNullItems_skipsItemMapping() {
        Purchase domain = new Purchase(UUID.randomUUID(), null, null, Instant.now(), null, null, null, null);

        PurchaseEntity entity = mapper.toEntity(domain);

        assertThat(entity.getItems()).isNullOrEmpty();
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
