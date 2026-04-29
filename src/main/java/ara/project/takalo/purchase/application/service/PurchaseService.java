package ara.project.takalo.purchase.application.service;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseService implements PurchaseServicePort {

    private static final String PERM_READ_ANY = "PERM_purchase:read:any";

    private final PurchaseRepository repository;
    private final ProductServicePort productService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Purchase create(Purchase purchase) {
        Purchase withOwner = purchase.ownerId() == null
                ? purchase.withOwner(currentUserProvider.id())
                : purchase;
        Purchase purchaseToSave = getPurchaseWithProductName(withOwner);
        return repository.save(purchaseToSave);
    }

    @Override
    public Purchase update(UUID id, Purchase purchase) {
        return repository.findById(id).map(existing -> {
            Purchase withOwner = purchase.withOwner(existing.ownerId());
            Purchase purchaseToSave = getPurchaseWithProductName(withOwner);
            return repository.save(purchaseToSave);
        }).orElseThrow(() -> new ResourceNotFoundException("Achat non trouvé"));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Purchase getById(UUID id) {
        Purchase purchase = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achat non trouvé"));
        if (!currentUserProvider.hasAuthority(PERM_READ_ANY)
                && !purchase.ownerId().equals(currentUserProvider.id())) {
            throw new AccessDeniedException("Accès refusé à cet achat");
        }
        return purchase;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<Purchase> search(Instant start, Instant end, int page, int limit) {
        if (currentUserProvider.hasAuthority(PERM_READ_ANY)) {
            return repository.findByDateRange(start, end, page, limit);
        }
        return repository.findByDateRangeAndOwner(start, end, currentUserProvider.id(), page, limit);
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
                purchase.ownerId(),
                purchase.purchaseDate(),
                enrichedItems
        );
    }
}
