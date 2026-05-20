package ara.project.takalo.verification.application.port.in;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.verification.domain.model.Verification;

import java.time.LocalDate;
import java.util.UUID;

public interface VerificationServicePort {

    /**
     * Crée une vérification de caisse. Si {@link VerificationCreateCommand#regularization()} est
     * non nul, déclenche dans la même transaction la création de l'achat ou du crédit budget
     * correspondant. 400 si la somme des coupures ne correspond pas au {@code countedTotal} ou
     * si le mode de régularisation ne correspond pas au signe de la différence. 403 si l'utilisateur
     * n'est pas éditeur du budget. 404 si le budget n'existe pas.
     */
    Verification create(VerificationCreateCommand command);

    /**
     * Liste paginée des vérifications. Restreinte aux vérifications créées par l'utilisateur
     * courant (sauf super-admin).
     */
    PagedResponse<Verification> search(UUID budgetId, LocalDate start, LocalDate end, int page, int size);

    /** Vérification par identifiant. 404 si introuvable. 403 si non éditeur du budget rattaché. */
    Verification getById(UUID id);
}
