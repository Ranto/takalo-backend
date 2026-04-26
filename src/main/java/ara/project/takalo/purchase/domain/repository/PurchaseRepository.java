package ara.project.takalo.purchase.domain.repository;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository {
    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID purchaseId);

    PagedResponse<Purchase> findAll(int page, int size);

    PagedResponse<Purchase> findByDateRange(Instant start, Instant end, int page, int size);

    void deleteById(UUID purchaseId);

}
