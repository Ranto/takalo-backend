package ara.project.takalo.purchase.application.port.out;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository {
    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID purchaseId);

    PagedResponse<Purchase> findAll(int page, int size);

    PagedResponse<Purchase> findByDateRange(Instant start, Instant end, int page, int size);

    PagedResponse<Purchase> findByDateRangeAndOwner(Instant start, Instant end, UUID ownerId, int page, int size);

    List<Purchase> findByBudgetId(UUID budgetId);

    List<Purchase> findByIds(Collection<UUID> ids);

    void deleteById(UUID purchaseId);

}
