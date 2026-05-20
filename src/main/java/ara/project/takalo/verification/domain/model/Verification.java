package ara.project.takalo.verification.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Verification(
        UUID id,
        UUID budgetId,
        UUID ownerId,
        LocalDate verificationDate,
        String note,
        BigDecimal referenceRest,
        BigDecimal countedTotal,
        BigDecimal difference,
        List<DenominationCount> denominations,
        RegularizationKind regularizationKind,
        UUID regularizationPurchaseId,
        UUID regularizationCreditMovementId,
        Instant createdAt,
        UUID createdBy
) {
    public Verification withRegularization(RegularizationKind kind, UUID purchaseId, UUID creditMovementId) {
        return new Verification(
                id, budgetId, ownerId, verificationDate, note,
                referenceRest, countedTotal, difference, denominations,
                kind, purchaseId, creditMovementId,
                createdAt, createdBy
        );
    }

    public Verification withOwner(UUID newOwnerId) {
        return new Verification(
                id, budgetId, newOwnerId, verificationDate, note,
                referenceRest, countedTotal, difference, denominations,
                regularizationKind, regularizationPurchaseId, regularizationCreditMovementId,
                createdAt, createdBy
        );
    }
}
