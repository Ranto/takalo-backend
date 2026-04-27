package ara.project.takalo.purchase.application.port.out;

import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.ParsedPurchaseRow;

import java.io.InputStream;
import java.util.List;

public interface PurchaseImportParser {
    boolean supports(ImportFormat format);

    List<ParsedPurchaseRow> parse(InputStream source);
}
