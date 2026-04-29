package ara.project.takalo.budget.domain.model;

public enum BudgetMovementType {
    ASSIGNATION_ACHAT,
    DESASSIGNATION_ACHAT,
    CREDIT_EXTERNE,
    TRANSFERT_ENTRANT,
    TRANSFERT_SORTANT,
    /** Projection-only : fond initial à la création, jamais persisté. */
    CREATION,
    /** Projection-only : achat initialement associé à un budget (pas de mouvement persisté). */
    ACHAT
}
