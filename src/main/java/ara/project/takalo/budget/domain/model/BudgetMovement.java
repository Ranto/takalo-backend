package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un mouvement enregistré sur un budget.
 *
 * <p>Conventions :
 * <ul>
 *     <li>{@code amount} est signé : négatif pour une sortie ({@code ASSIGNATION_ACHAT},
 *     {@code TRANSFERT_SORTANT}), positif pour une entrée ({@code DESASSIGNATION_ACHAT},
 *     {@code TRANSFERT_ENTRANT}, {@code CREDIT_EXTERNE}).</li>
 *     <li>{@code correlationId} : non null pour les opérations à deux mouvements miroirs
 *     (réassignation d'achat, transfert inter-budget). Les deux mouvements miroirs
 *     partagent le même {@code correlationId}.</li>
 *     <li>{@code source} : non null uniquement pour {@code CREDIT_EXTERNE}.</li>
 *     <li>{@code counterpartBudgetId} : non null uniquement pour les transferts ; pointe
 *     vers le budget de l'autre côté de l'opération.</li>
 * </ul>
 */
public record BudgetMovement(
        UUID id,
        UUID budgetId,
        BudgetMovementType type,
        BigDecimal amount,
        Instant occurredAt,
        String reason,
        UUID correlationId,
        UUID purchaseId,
        BudgetMovementSource source,
        UUID counterpartBudgetId,
        UUID createdBy,
        Instant createdAt
) {
}
