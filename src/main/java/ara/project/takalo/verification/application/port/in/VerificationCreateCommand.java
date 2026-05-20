package ara.project.takalo.verification.application.port.in;

import ara.project.takalo.verification.domain.model.DenominationCount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VerificationCreateCommand(
        UUID budgetId,
        LocalDate verificationDate,
        String note,
        BigDecimal referenceRest,
        BigDecimal countedTotal,
        List<DenominationCount> denominations,
        RegularizationCommand regularization
) {
}
