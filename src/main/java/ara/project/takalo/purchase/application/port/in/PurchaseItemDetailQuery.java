package ara.project.takalo.purchase.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record PurchaseItemDetailQuery(Instant start,
                                      Instant end,
                                      String productName,
                                      String categoryName,
                                      UUID budgetId,
                                      boolean includeUnbudgeted,
                                      UUID productId,
                                      UUID categoryId,
                                      SortField sort,
                                      SortDirection direction,
                                      int page,
                                      int size) {

    public enum SortField { DATE, PRODUCT, CATEGORY }

    public enum SortDirection { ASC, DESC }
}