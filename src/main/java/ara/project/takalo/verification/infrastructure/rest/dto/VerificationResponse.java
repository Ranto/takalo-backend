package ara.project.takalo.verification.infrastructure.rest.dto;

import ara.project.takalo.verification.domain.model.RegularizationKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VerificationResponse(
        UUID id,
        UUID budgetId,
        String budgetName,
        LocalDate verificationDate,
        String note,
        BigDecimal referenceRest,
        BigDecimal countedTotal,
        BigDecimal difference,
        List<DenominationCountResponse> denominations,
        RegularizationKind regularizationKind,
        UUID regularizationPurchaseId,
        UUID regularizationCreditId,
        Instant createdAt,
        UUID createdBy
) {
}
