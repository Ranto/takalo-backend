package ara.project.takalo.verification.application.port.out;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.verification.domain.model.Verification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface VerificationRepository {

    Verification save(Verification verification);

    Optional<Verification> findById(UUID id);

    /**
     * Recherche paginée filtrable. {@code ownerFilter} non nul restreint aux vérifications
     * dont {@code owner_id} est égal à cette valeur (utilisé pour la visibilité côté utilisateur
     * standard) ; {@code null} signifie aucune restriction (super-admin).
     */
    PagedResponse<Verification> search(UUID budgetId, LocalDate start, LocalDate end,
                                       UUID ownerFilter, int page, int size);
}
