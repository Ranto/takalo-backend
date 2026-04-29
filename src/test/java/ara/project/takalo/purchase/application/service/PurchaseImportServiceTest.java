package ara.project.takalo.purchase.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseImportParser;
import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.purchase.domain.model.ImportError;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseImportServiceTest {

    private PurchaseImportParser excelParser;
    private PurchaseServicePort purchaseService;
    private ProductServicePort productService;
    private PurchaseImportService service;

    private static final InputStream ANY_STREAM = new ByteArrayInputStream(new byte[0]);

    @BeforeEach
    void setUp() {
        excelParser = mock(PurchaseImportParser.class);
        purchaseService = mock(PurchaseServicePort.class);
        productService = mock(ProductServicePort.class);
        when(excelParser.supports(ImportFormat.EXCEL_XLSX)).thenReturn(true);
        service = new PurchaseImportService(List.of(excelParser), purchaseService, productService);
    }

    @Test
    void importPurchases_unsupportedFormat_throws() {
        when(excelParser.supports(ImportFormat.EXCEL_XLSX)).thenReturn(false);

        assertThatThrownBy(() -> service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX))
                .isInstanceOf(UnsupportedImportFormatException.class);
        verify(purchaseService, never()).create(any());
    }

    @Test
    void importPurchases_groupsItemsByDate_andCreatesOnePurchasePerDate() {
        UUID laitId = UUID.randomUUID();
        UUID painId = UUID.randomUUID();
        LocalDate d1 = LocalDate.of(2026, 4, 27);
        LocalDate d2 = LocalDate.of(2026, 4, 28);

        when(excelParser.parse(any())).thenReturn(List.of(
                row(2, d1, "Lait", 2.0, "1.50"),
                row(3, d2, "Pain", 1.0, "0.90"),
                row(4, d1, "Lait", 3.0, "1.50")
        ));
        when(productService.findIdByName("Lait")).thenReturn(Optional.of(laitId));
        when(productService.findIdByName("Pain")).thenReturn(Optional.of(painId));
        when(purchaseService.create(any())).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            return new Purchase(UUID.randomUUID(), p.ownerId(), p.purchaseDate(), p.items());
        });

        PurchaseImportResult result = service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX);

        assertThat(result.errors()).isEmpty();
        assertThat(result.imported()).hasSize(2);
        assertThat(result.imported().get(0).purchaseDate())
                .isEqualTo(d1.atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(result.imported().get(0).items()).hasSize(2);
        assertThat(result.imported().get(1).purchaseDate())
                .isEqualTo(d2.atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(result.imported().get(1).items()).hasSize(1);

        verify(purchaseService, times(2)).create(any());
    }

    @Test
    void importPurchases_collectsErrorsAndKeepsValidRows() {
        UUID laitId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 27);

        when(excelParser.parse(any())).thenReturn(List.of(
                row(2, date, "Lait", 2.0, "1.50"),
                row(3, null, "Pain", 1.0, "0.90"),
                row(4, date, " ", 1.0, "0.90"),
                row(5, date, "Sucre", null, "1.00"),
                row(6, date, "Sel", 0.0, "1.00"),
                row(7, date, "Riz", 1.0, "-1.00"),
                row(8, date, "Inconnu", 1.0, "1.00")
        ));
        when(productService.findIdByName("Lait")).thenReturn(Optional.of(laitId));
        when(productService.findIdByName("Inconnu")).thenReturn(Optional.empty());
        when(purchaseService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseImportResult result = service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX);

        assertThat(result.imported()).hasSize(1);
        assertThat(result.imported().getFirst().items()).hasSize(1);

        assertThat(result.errors())
                .extracting(ImportError::lineNumber, ImportError::errorMessage)
                .containsExactly(
                        tuple(3, "Date d'achat manquante"),
                        tuple(4, "Nom du produit manquant"),
                        tuple(5, "Quantité invalide"),
                        tuple(6, "Quantité invalide"),
                        tuple(7, "Prix unitaire invalide"),
                        tuple(8, "Produit introuvable : Inconnu")
                );
    }

    @Test
    void importPurchases_noValidRows_returnsEmptyImportedAndDoesNotCallCreate() {
        when(excelParser.parse(any())).thenReturn(List.of(
                row(2, null, "Lait", 1.0, "1.00")
        ));

        PurchaseImportResult result = service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX);

        assertThat(result.imported()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        verify(purchaseService, never()).create(any());
    }

    @Test
    void importPurchases_passesFullPurchaseItemFields() {
        UUID productId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 27);

        ParsedPurchaseRow detailed = new ParsedPurchaseRow(
                2, date, "Lait", 2.0,
                new BigDecimal("1.50"), new BigDecimal("0.20"),
                LocalDate.of(2026, 12, 31), "Carrefour"
        );
        when(excelParser.parse(any())).thenReturn(List.of(detailed));
        when(productService.findIdByName("Lait")).thenReturn(Optional.of(productId));
        when(purchaseService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseImportResult result = service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX);

        PurchaseItem item = result.imported().getFirst().items().getFirst();
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.quantity()).isEqualTo(2.0);
        assertThat(item.unitPrice()).isEqualByComparingTo("1.50");
        assertThat(item.discount()).isEqualByComparingTo("0.20");
        assertThat(item.expiryDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(item.storeName()).isEqualTo("Carrefour");
        assertThat(item.productName()).isEqualTo("Lait");
    }

    @Test
    void importPurchases_nullDiscount_defaultsToZero() {
        UUID productId = UUID.randomUUID();
        when(excelParser.parse(any())).thenReturn(List.of(
                new ParsedPurchaseRow(2, LocalDate.of(2026, 4, 27), "Lait", 1.0,
                        new BigDecimal("1.00"), null, null, null)
        ));
        when(productService.findIdByName("Lait")).thenReturn(Optional.of(productId));
        when(purchaseService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseImportResult result = service.importPurchases(ANY_STREAM, ImportFormat.EXCEL_XLSX);

        assertThat(result.imported().getFirst().items().getFirst().discount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static ParsedPurchaseRow row(int line, LocalDate date, String name, Double qty, String price) {
        return new ParsedPurchaseRow(
                line, date, name, qty,
                price == null ? null : new BigDecimal(price),
                BigDecimal.ZERO, null, null
        );
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.api.Assertions.tuple(values);
    }
}