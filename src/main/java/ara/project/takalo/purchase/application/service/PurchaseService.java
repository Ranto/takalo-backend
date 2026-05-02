package ara.project.takalo.purchase.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.purchase.application.port.in.BulkReassignBudgetResult;
import ara.project.takalo.purchase.application.port.in.PurchaseItemDetailQuery;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
        Purchase purchaseToSave = resolveProductReferences(withOwner);
        return repository.save(purchaseToSave);
    }

    @Override
    public Purchase update(UUID id, Purchase purchase) {
        return repository.findById(id).map(existing -> {
            Purchase merged = new Purchase(
                    existing.id(),
                    existing.ownerId(),
                    existing.budgetId(),
                    purchase.purchaseDate(),
                    purchase.notes(),
                    purchase.items()
            );
            Purchase purchaseToSave = resolveProductReferences(merged);
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
    @Transactional(readOnly = true)
    public PagedResponse<PurchaseItemDetail> searchItemDetails(PurchaseItemDetailQuery query) {
        UUID ownerFilter = currentUserProvider.hasAuthority(PERM_READ_ANY)
                ? null
                : currentUserProvider.id();
        return repository.searchItemDetails(query, ownerFilter);
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

        return applyBudgetReassignment(existing, newBudgetId, date, raison, UUID.randomUUID());
    }

    @Override
    public BulkReassignBudgetResult reassignBudgetBulk(Collection<UUID> purchaseIds, UUID newBudgetId,
                                                       Instant date, String raison) {
        Set<UUID> uniqueIds = new LinkedHashSet<>(purchaseIds);
        List<Purchase> found = repository.findByIds(uniqueIds);
        if (found.size() != uniqueIds.size()) {
            Set<UUID> foundIds = found.stream().map(Purchase::id).collect(Collectors.toSet());
            List<UUID> missing = uniqueIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ResourceNotFoundException("Achats non trouvés : " + missing);
        }

        UUID currentUserId = currentUserProvider.id();
        for (Purchase p : found) {
            if (!p.ownerId().equals(currentUserId)) {
                throw new AccessDeniedException("Accès refusé à l'achat " + p.id());
            }
        }

        if (newBudgetId != null) {
            requireBudgetEditor(newBudgetId);
        }
        Set<UUID> oldBudgetIds = found.stream()
                .map(Purchase::budgetId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        for (UUID oldBudgetId : oldBudgetIds) {
            requireBudgetEditor(oldBudgetId);
        }

        UUID correlationId = UUID.randomUUID();
        Map<UUID, Purchase> byId = found.stream().collect(Collectors.toMap(Purchase::id, p -> p));
        List<Purchase> updated = new ArrayList<>(uniqueIds.size());
        for (UUID id : uniqueIds) {
            updated.add(applyBudgetReassignment(byId.get(id), newBudgetId, date, raison, correlationId));
        }
        return new BulkReassignBudgetResult(updated, correlationId);
    }

    private Purchase applyBudgetReassignment(Purchase existing, UUID newBudgetId,
                                             Instant date, String raison, UUID correlationId) {
        UUID oldBudgetId = existing.budgetId();
        Purchase updated = repository.save(existing.withBudget(newBudgetId));
        java.math.BigDecimal amount = existing.getTotalAmount();
        if (oldBudgetId != null) {
            budgetService.recordPurchaseUnassignment(oldBudgetId, existing.id(), amount, date, raison, correlationId);
        }
        if (newBudgetId != null) {
            budgetService.recordPurchaseAssignment(newBudgetId, existing.id(), amount, date, raison, correlationId);
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> findByBudgetId(UUID budgetId) {
        return repository.findByBudgetId(budgetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> findByIds(Collection<UUID> ids) {
        return repository.findByIds(ids);
    }

    private void requireBudgetEditor(UUID budgetId) {
        Budget budget = budgetService.getRawById(budgetId);
        Set<UUID> editorIds = budget.editorIds() == null ? Set.of() : budget.editorIds();
        if (!editorIds.contains(currentUserProvider.id())) {
            throw new ForbiddenException("Vous n'êtes pas éditeur de ce budget");
        }
    }

    private @NonNull Purchase resolveProductReferences(Purchase purchase) {
        List<PurchaseItem> resolvedItems = purchase.items().stream().map(item -> {
            Product product = productService.findOrCreateByName(item.productName());
            return new PurchaseItem(
                    product.id(),
                    item.quantity(),
                    item.unitPrice(),
                    item.discount(),
                    item.expiryDate(),
                    item.storeName(),
                    product.name()
            );
        }).toList();

        return new Purchase(
                purchase.id(),
                purchase.ownerId(),
                purchase.budgetId(),
                purchase.purchaseDate(),
                purchase.notes(),
                resolvedItems
        );
    }
}
