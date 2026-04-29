package ara.project.takalo.budget.application.port.in;

import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.shared.domain.utility.PagedResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BudgetServicePort {
    Budget create(Budget budget);

    BudgetWithBalance getById(UUID id);

    PagedResponse<BudgetWithBalance> findAll(int page, int size);

    BudgetWithBalance update(UUID id, BudgetUpdateCommand command);

    /** Renvoie le budget brut (sans calcul de reste). 404 si inconnu. */
    Budget getRawById(UUID id);

    /**
     * Enregistre un mouvement {@code ASSIGNATION_ACHAT} sur le budget cible.
     * Vérifie que le budget existe (404) et que l'utilisateur courant est éditeur (403).
     */
    void recordPurchaseAssignment(UUID budgetId, UUID purchaseId, BigDecimal amount,
                                  Instant date, String reason, UUID correlationId);

    /**
     * Enregistre un mouvement {@code DESASSIGNATION_ACHAT} sur le budget source.
     * Vérifie que le budget existe (404) et que l'utilisateur courant est éditeur (403).
     */
    void recordPurchaseUnassignment(UUID budgetId, UUID purchaseId, BigDecimal amount,
                                    Instant date, String reason, UUID correlationId);

    /** Crédit depuis une source externe. Mute le fond du budget. */
    BudgetWithBalance creditFromExternalSource(UUID budgetId, BudgetCreditCommand command);

    /** Transfert entre deux budgets. Mute les deux fonds, dans la même transaction. */
    BudgetTransferResult transfer(BudgetTransferCommand command);

    /** Liste des mouvements d'un budget, filtrable par type. Lecture ouverte. */
    List<BudgetMovement> findMovements(UUID budgetId, BudgetMovementType typeFilter);
}
