package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.repository.PurchaseRepository;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PurchasePersistenceAdapter implements PurchaseRepository {

    private final JpaPurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;

    @Override
    public Purchase save(Purchase purchase) {
        PurchaseEntity entity = purchaseMapper.toEntity(purchase);
        PurchaseEntity entitySave = purchaseRepository.save(entity);
        return purchaseMapper.toDomain(entitySave);
    }

    @Override
    public Optional<Purchase> findById(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId).map(purchaseMapper::toDomain);
    }

    @Override
    public PagedResponse<Purchase> findAll(int page, int size) {
        Page<PurchaseEntity> resultPage = purchaseRepository.findAll(PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(resultPage, purchaseMapper::toDomain);
    }

    @Override
    public PagedResponse<Purchase> findByDateRange(Instant start, Instant end, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entityPage = purchaseRepository.findByDateRange(start, end, pageable);

        return PaginationMapper.toPagedResponse(entityPage, purchaseMapper::toDomain);
    }

    @Override
    public void deleteById(UUID purchaseId) {
        if (purchaseRepository.existsById(purchaseId)) {
            purchaseRepository.deleteById(purchaseId);
        }
    }

}
