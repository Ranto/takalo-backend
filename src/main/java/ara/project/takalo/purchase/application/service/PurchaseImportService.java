package ara.project.takalo.purchase.application.service;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.purchase.application.port.in.PurchaseImportServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseImportParser;
import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.purchase.domain.model.ImportError;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseImportPreviewItem;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.domain.model.PurchaseImportValidationResult;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseImportService implements PurchaseImportServicePort {

    private final List<PurchaseImportParser> parsers;
    private final PurchaseServicePort purchaseService;
    private final ProductServicePort productService;
    private final ProductCategoryServicePort categoryService;

    @Override
    public PurchaseImportResult importPurchases(InputStream source, ImportFormat format) {
        List<ParsedPurchaseRow> parsedRows = parseRows(source, format);

        List<ImportError> errors = new ArrayList<>();
        Map<LocalDate, List<PurchaseItem>> itemsByDate = new LinkedHashMap<>();

        for (ParsedPurchaseRow row : parsedRows) {
            try {
                validateRowFields(row);
                PurchaseItem item = parsedRowToPurchaseItem(row);
                itemsByDate.computeIfAbsent(row.purchaseDate(), d -> new ArrayList<>()).add(item);
            } catch (ImportRowException e) {
                errors.add(new ImportError(row.lineNumber(), e.rawValue, e.getMessage(), row));
            }
        }

        List<Purchase> imported = new ArrayList<>();
        for (Map.Entry<LocalDate, List<PurchaseItem>> entry : itemsByDate.entrySet()) {
            Purchase toCreate = new Purchase(
                    null,
                    null,
                    null,
                    entry.getKey().atStartOfDay(ZoneOffset.UTC).toInstant(),
                    null,
                    null,
                    null,
                    entry.getValue()
            );
            imported.add(purchaseService.create(toCreate));
        }

        return new PurchaseImportResult(imported, errors);
    }

    @Transactional(readOnly = true)
    @Override
    public PurchaseImportValidationResult validate(InputStream source, ImportFormat format) {
        List<ParsedPurchaseRow> parsedRows = parseRows(source, format);

        List<ImportError> errors = new ArrayList<>();
        List<PurchaseImportPreviewItem> validRows = new ArrayList<>();

        for (ParsedPurchaseRow row : parsedRows) {
            try {
                validateRowFields(row);
                validRows.add(toPreviewItem(row));
            } catch (ImportRowException e) {
                errors.add(new ImportError(row.lineNumber(), e.rawValue, e.getMessage(), row));
            }
        }

        return new PurchaseImportValidationResult(validRows, errors);
    }

    private List<ParsedPurchaseRow> parseRows(InputStream source, ImportFormat format) {
        PurchaseImportParser parser = parsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> new UnsupportedImportFormatException(format));
        return parser.parse(source);
    }

    private void validateRowFields(ParsedPurchaseRow row) {
        if (row.purchaseDate() == null) {
            throw new ImportRowException(null, "Date d'achat manquante");
        }
        if (row.productName() == null || row.productName().isBlank()) {
            throw new ImportRowException(null, "Nom du produit manquant");
        }
        if (row.quantity() == null || row.quantity() <= 0) {
            throw new ImportRowException(row.quantity() == null ? null : String.valueOf(row.quantity()), "Quantité invalide");
        }
        if (row.unitPrice() == null || row.unitPrice().signum() < 0) {
            throw new ImportRowException(row.unitPrice() == null ? null : row.unitPrice().toPlainString(), "Prix unitaire invalide");
        }
    }

    private PurchaseItem parsedRowToPurchaseItem(ParsedPurchaseRow row) {
        UUID productId = productService.findIdByName(row.productName())
                .orElseGet(() -> createProductFromRow(row));

        BigDecimal discount = row.discount() == null ? BigDecimal.ZERO : row.discount();

        return new PurchaseItem(
                productId,
                row.quantity(),
                row.unitPrice(),
                discount,
                row.expiryDate(),
                row.storeName(),
                row.productName()
        );
    }

    private PurchaseImportPreviewItem toPreviewItem(ParsedPurchaseRow row) {
        Optional<UUID> productId = productService.findIdByName(row.productName());
        Optional<UUID> categoryId = categoryService.findIdByLabel(row.categoryName());
        boolean categoryProvided = row.categoryName() != null && !row.categoryName().isBlank();

        BigDecimal discount = row.discount() == null ? BigDecimal.ZERO : row.discount();

        return new PurchaseImportPreviewItem(
                row.lineNumber(),
                row.purchaseDate(),
                row.productName(),
                productId.orElse(null),
                productId.isPresent(),
                row.categoryName(),
                categoryId.orElse(null),
                categoryProvided && categoryId.isPresent(),
                row.quantity(),
                row.unitPrice(),
                discount
        );
    }

    private UUID createProductFromRow(ParsedPurchaseRow row) {
        UUID categoryId = null;
        if (row.categoryName() != null && !row.categoryName().isBlank()) {
            categoryId = categoryService.findOrCreateByLabel(row.categoryName()).id();
        }
        Product created = productService.create(new Product(null, row.productName(), categoryId, null, null));
        return created.id();
    }

    private static class ImportRowException extends RuntimeException {
        private final String rawValue;

        ImportRowException(String rawValue, String message) {
            super(message);
            this.rawValue = rawValue;
        }
    }
}
