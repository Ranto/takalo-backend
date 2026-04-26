package ara.project.takalo.purchase.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.domain.repository.PurchaseRepository;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService implements PurchaseServicePort {

    private final PurchaseRepository repository;
    private final ProductServicePort productService;

    @Override
    @Transactional
    public Purchase create(Purchase purchase) {
        Purchase purchaseToSave = getPurchaseWithProductName(purchase);
        return repository.save(purchaseToSave);
    }

    @Override
    @Transactional
    public Purchase update(UUID id, Purchase purchase) {
        repository.findById(id).map(existing -> {
            Purchase updated = new Purchase(id, purchase.purchaseDate(), purchase.items());
            return repository.save(updated);
        }).orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        return null;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Purchase getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Purchase> search(Instant start, Instant end, int page, int limit) {
        return repository.findByDateRange(start, end, page, limit);
    }

    private @NonNull Purchase getPurchaseWithProductName(Purchase purchase) {
        Set<UUID> productIds = purchase.items().stream()
                .map(PurchaseItem::productId)
                .collect(Collectors.toSet());

        Map<UUID, String> productMapNames = productService.getProductNames(productIds);

        List<PurchaseItem> enrichedItems = purchase.items().stream().map(
                item -> {
                    String productName = productMapNames.getOrDefault(item.productId(), "Produit supprimé");
                    return new PurchaseItem(
                            item.productId(),
                            item.quantity(),
                            item.unitPrice(),
                            item.discount(),
                            item.expiryDate(),
                            item.storeName(),
                            productName
                    );
                }
        ).toList();

        return new Purchase(
                purchase.id(),
                purchase.purchaseDate(),
                enrichedItems
        );
    }
}
