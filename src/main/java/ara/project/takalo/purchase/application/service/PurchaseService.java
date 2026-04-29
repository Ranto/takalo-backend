package ara.project.takalo.purchase.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
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
    private final BudgetServicePort budgetService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Purchase create(Purchase purchase) {
        Purchase withOwner = purchase.ownerId() == null
                ? purchase.withOwner(currentUserProvider.id())
                : purchase;
        if (withOwner.budgetId() != null) {
            requireBudgetEditor(withOwner.budgetId());
        }
        Purchase purchaseToSave = getPurchaseWithProductName(withOwner);
        return repository.save(purchaseToSave);
    }

    @Override
    public Purchase update(UUID id, Purchase purchase) {
        return repository.findById(id).map(existing -> {
            Purchase withOwner = purchase.withOwner(existing.ownerId())
                    .withBudget(existing.budgetId());
            Purchase purchaseToSave = getPurchaseWithProductName(withOwner);
            return repository.save(purchaseToSave);
        }).orElseThrow(() -> new ResourceNotFoundException("Achat non trouvé"));
    }

    @Override
    public void delete(UUID id) {
        Purchase existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achat non trouvé"));
        if (!existing.ownerId().equals(currentUserProvider.id())
                && !currentUserProvider.hasAuthority(PERM_READ_ANY)) {
            throw new AccessDeniedException("Accès refusé à cet achat");
        }
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

    @Override
    public Purchase reassignBudget(UUID purchaseId, UUID newBudgetId, Instant date, String raison) {
        Purchase existing = repository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Achat non trouvé"));

        UUID currentUserId = currentUserProvider.id();
        if (!existing.ownerId().equals(currentUserId)) {
            throw new AccessDeniedException("Accès refusé à cet achat");
        }

        UUID oldBudgetId = existing.budgetId();
        if (oldBudgetId != null) {
            requireBudgetEditor(oldBudgetId);
        }
        if (newBudgetId != null) {
            requireBudgetEditor(newBudgetId);
        }

        Purchase updated = repository.save(existing.withBudget(newBudgetId));

        UUID correlationId = UUID.randomUUID();
        java.math.BigDecimal amount = existing.getTotalAmount();
        if (oldBudgetId != null) {
            budgetService.recordPurchaseUnassignment(oldBudgetId, purchaseId, amount, date, raison, correlationId);
        }
        if (newBudgetId != null) {
            budgetService.recordPurchaseAssignment(newBudgetId, purchaseId, amount, date, raison, correlationId);
        }
        return updated;
    }

    private void requireBudgetEditor(UUID budgetId) {
        Budget budget = budgetService.getRawById(budgetId);
        Set<UUID> editorIds = budget.editorIds() == null ? Set.of() : budget.editorIds();
        if (!editorIds.contains(currentUserProvider.id())) {
            throw new ForbiddenException("Vous n'êtes pas éditeur de ce budget");
        }
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
                purchase.budgetId(),
                purchase.purchaseDate(),
                enrichedItems
        );
    }
}
