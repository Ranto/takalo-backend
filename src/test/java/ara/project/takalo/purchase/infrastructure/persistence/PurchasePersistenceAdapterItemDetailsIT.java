package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import ara.project.takalo.purchase.application.port.in.PurchaseItemDetailQuery;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseItemEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseItemMapper;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Import({
        PurchasePersistenceAdapter.class,
        PurchaseMapper.class,
        PurchaseItemMapper.class,
        PurchasePersistenceAdapterItemDetailsIT.TestConfig.class
})
class PurchasePersistenceAdapterItemDetailsIT {

    static class TestConfig {
        @Bean("auditorAware")
        AuditorAware<UUID> auditorAware() {
            return () -> Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        }
    }

    @Autowired
    private PurchasePersistenceAdapter adapter;

    @PersistenceContext
    private EntityManager em;

    private UUID aliceId;
    private UUID bobId;
    private UUID dairyId;
    private UUID bakeryId;
    private UUID milkId;
    private UUID breadId;
    private UUID orphanProductId;

    @BeforeEach
    void seed() {
        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();

        ProductCategoryEntity dairy = ProductCategoryEntity.builder().label("Produits laitiers").build();
        ProductCategoryEntity bakery = ProductCategoryEntity.builder().label("Boulangerie").build();
        em.persist(dairy);
        em.persist(bakery);
        dairyId = dairy.getId();
        bakeryId = bakery.getId();

        ProductEntity milk = ProductEntity.builder().name("Lait").categoryId(dairyId).build();
        ProductEntity bread = ProductEntity.builder().name("Pain").categoryId(bakeryId).build();
        em.persist(milk);
        em.persist(bread);
        milkId = milk.getId();
        breadId = bread.getId();
        orphanProductId = UUID.randomUUID();

        // Achat 1 (Alice, ancien) : 2 lignes Lait + Pain.
        // Note : les items sont persistés individuellement car PurchaseEntity#addItem utilise
        // un HashSet qui déduplique les items tant que leur id (clé d'égalité) est null.
        PurchaseEntity p1 = PurchaseEntity.builder()
                .ownerId(aliceId)
                .purchaseDate(Instant.parse("2026-01-10T08:00:00Z"))
                .build();
        em.persist(p1);
        persistItem(p1, milkId, "Lait", "2.00", 2.0, "0.00", "Carrefour");
        persistItem(p1, breadId, "Pain", "1.50", 1.0, "0.00", "Carrefour");

        // Achat 2 (Bob, récent) : ligne Lait + ligne avec produit supprimé
        PurchaseEntity p2 = PurchaseEntity.builder()
                .ownerId(bobId)
                .purchaseDate(Instant.parse("2026-04-20T10:00:00Z"))
                .build();
        em.persist(p2);
        persistItem(p2, milkId, "Lait", "2.10", 3.0, "0.30", "Leclerc");
        persistItem(p2, orphanProductId, "Yaourt supprimé", "1.20", 4.0, "0.00", "Leclerc");

        em.flush();
        em.clear();
    }

    private void persistItem(PurchaseEntity purchase, UUID productId, String productName,
                             String unitPrice, double quantity, String discount, String store) {
        PurchaseItemEntity item = PurchaseItemEntity.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .discount(new BigDecimal(discount))
                .storeName(store)
                .build();
        item.setPurchase(purchase);
        em.persist(item);
    }

    @Test
    void searchItemDetails_sortedByDateDesc_returnsAllLines() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.content()).hasSize(4);
        // Les 2 premières lignes sont celles de l'achat de 2026-04-20 (Bob)
        assertThat(page.content().get(0).purchaseDate()).isEqualTo(Instant.parse("2026-04-20T10:00:00Z"));
        assertThat(page.content().get(1).purchaseDate()).isEqualTo(Instant.parse("2026-04-20T10:00:00Z"));
        assertThat(page.content().get(2).purchaseDate()).isEqualTo(Instant.parse("2026-01-10T08:00:00Z"));
    }

    @Test
    void searchItemDetails_filteredByOwner_restrictsToAlice() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, aliceId);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(PurchaseItemDetail::productName)
                .containsExactlyInAnyOrder("Lait", "Pain");
    }

    @Test
    void searchItemDetails_filteredByCategoryName_joinsToCurrentCategory() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, "laitiers",
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).allMatch(l -> "Lait".equals(l.productName()));
        assertThat(page.content()).allMatch(l -> dairyId.equals(l.categoryId()));
    }

    @Test
    void searchItemDetails_filteredByProductName_isCaseInsensitive() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, "PAIN", null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().productName()).isEqualTo("Pain");
    }

    @Test
    void searchItemDetails_filteredByDateRange() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                null, null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).allMatch(l ->
                l.purchaseDate().equals(Instant.parse("2026-04-20T10:00:00Z")));
    }

    @Test
    void searchItemDetails_sortedByCategoryAsc_groupsByCategoryLabel() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, null,
                PurchaseItemDetailQuery.SortField.CATEGORY,
                PurchaseItemDetailQuery.SortDirection.ASC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        // Ordre attendu (ASC) : null (produit supprimé) -> "Boulangerie" -> "Produits laitiers"
        // H2/Postgres traitent NULL comme inférieur en ASC par défaut.
        assertThat(page.content()).hasSize(4);
        // Vérifie regroupement : toutes les lignes "Produits laitiers" sont consécutives
        long lastDairyIndex = -1;
        long firstDairyIndex = -1;
        for (int i = 0; i < page.content().size(); i++) {
            if ("Produits laitiers".equals(page.content().get(i).categoryLabel())) {
                if (firstDairyIndex == -1) firstDairyIndex = i;
                lastDairyIndex = i;
            }
        }
        assertThat(lastDairyIndex - firstDairyIndex).isEqualTo(1);
    }

    @Test
    void searchItemDetails_orphanProduct_returnsLineWithNullCategory() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, "supprimé", null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(1);
        PurchaseItemDetail line = page.content().getFirst();
        assertThat(line.productName()).isEqualTo("Yaourt supprimé");
        assertThat(line.categoryId()).isNull();
        assertThat(line.categoryLabel()).isNull();
    }

    @Test
    void searchItemDetails_pagination_capsResultsAndComputesTotalPages() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, null, null,
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.DESC,
                0, 2);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, null);

        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.content()).hasSize(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void searchItemDetails_total_isComputedFromUnitPriceQuantityDiscount() {
        PurchaseItemDetailQuery q = new PurchaseItemDetailQuery(
                null, null, "Lait", "laitiers",
                PurchaseItemDetailQuery.SortField.DATE,
                PurchaseItemDetailQuery.SortDirection.ASC,
                0, 10);

        PagedResponse<PurchaseItemDetail> page = adapter.searchItemDetails(q, bobId);

        assertThat(page.totalElements()).isEqualTo(1);
        // 2.10 * 3 - 0.30 = 6.00
        assertThat(page.content().getFirst().total()).isEqualByComparingTo(new BigDecimal("6.00"));
    }
}
