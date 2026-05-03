package ara.project.takalo.purchase.application.port.in;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurchaseServicePort {
    Purchase create(Purchase purchase);

    Purchase update(UUID id, Purchase purchase);

    void delete(UUID id);

    /** Verrouille un achat. Seul l'auteur de l'achat ou un SUPER_ADMIN peut verrouiller. */
    Purchase lock(UUID id);

    /** Déverrouille un achat. Réservé au SUPER_ADMIN. */
    Purchase unlock(UUID id);

    /**
     * Verrouille plusieurs achats en une seule opération atomique. Si l'un des achats échoue
     * (introuvable, non-auteur et non SUPER_ADMIN), aucune modification n'est appliquée.
     * Les achats déjà verrouillés sont laissés inchangés.
     */
    List<Purchase> lockBulk(Collection<UUID> purchaseIds);

    /**
     * Déverrouille plusieurs achats en une seule opération atomique. Réservé au SUPER_ADMIN.
     * Les achats non verrouillés sont laissés inchangés.
     */
    List<Purchase> unlockBulk(Collection<UUID> purchaseIds);

    Purchase getById(UUID id);

    PagedResponse<Purchase> search(Instant start, Instant end, int page, int limit);

    PagedResponse<PurchaseItemDetail> searchItemDetails(PurchaseItemDetailQuery query);

    Purchase reassignBudget(UUID purchaseId, UUID newBudgetId, Instant date, String raison);

    BulkReassignBudgetResult reassignBudgetBulk(Collection<UUID> purchaseIds, UUID newBudgetId,
                                                Instant date, String raison);

    /** Achats actuellement associés au budget donné. Lecture interne, sans contrôle d'auteur. */
    List<Purchase> findByBudgetId(UUID budgetId);

    /** Achats par identifiants (les inconnus sont silencieusement omis). Lecture interne. */
    List<Purchase> findByIds(Collection<UUID> ids);
}
