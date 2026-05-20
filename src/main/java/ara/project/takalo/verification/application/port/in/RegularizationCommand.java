package ara.project.takalo.verification.application.port.in;

import ara.project.takalo.verification.domain.model.RegularizationKind;

import java.time.LocalDate;

/**
 * Demande de régularisation automatique d'une vérification de caisse :
 * création d'un achat ({@link RegularizationKind#PURCHASE}) si manquant, ou
 * crédit du budget ({@link RegularizationKind#CREDIT}) si excédent.
 */
public record RegularizationCommand(
        RegularizationKind kind,
        String label,
        LocalDate date
) {
}
