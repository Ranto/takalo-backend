package ara.project.takalo.purchase.application.port.in;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.time.Instant;
import java.util.UUID;

public interface PurchaseServicePort {
    Purchase create(Purchase purchase);

    Purchase update(UUID id, Purchase purchase);

    void delete(UUID id);

    Purchase getById(UUID id);

    PagedResponse<Purchase> search(Instant start, Instant end, int page, int limit);

    Purchase reassignBudget(UUID purchaseId, UUID newBudgetId, Instant date, String raison);
}
