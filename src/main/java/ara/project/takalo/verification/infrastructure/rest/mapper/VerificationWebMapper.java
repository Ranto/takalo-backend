package ara.project.takalo.verification.infrastructure.rest.mapper;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.verification.application.port.in.RegularizationCommand;
import ara.project.takalo.verification.application.port.in.VerificationCreateCommand;
import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.rest.dto.DenominationCountRequest;
import ara.project.takalo.verification.infrastructure.rest.dto.DenominationCountResponse;
import ara.project.takalo.verification.infrastructure.rest.dto.RegularizationRequest;
import ara.project.takalo.verification.infrastructure.rest.dto.VerificationRequest;
import ara.project.takalo.verification.infrastructure.rest.dto.VerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VerificationWebMapper {

    private final BudgetServicePort budgetService;

    public VerificationCreateCommand toCommand(VerificationRequest request) {
        List<DenominationCount> denominations = request.denominations() == null
                ? List.of()
                : request.denominations().stream()
                .map(d -> new DenominationCount(d.value(), d.quantity()))
                .toList();
        RegularizationCommand regularization = null;
        RegularizationRequest reg = request.regularization();
        if (reg != null) {
            regularization = new RegularizationCommand(reg.kind(), reg.label(), reg.date());
        }
        return new VerificationCreateCommand(
                request.budgetId(),
                request.verificationDate(),
                request.note(),
                request.referenceRest(),
                request.countedTotal(),
                denominations,
                regularization
        );
    }

    public VerificationResponse toResponse(Verification verification) {
        String budgetName = resolveBudgetName(verification.budgetId());
        List<DenominationCountResponse> denominations = verification.denominations() == null
                ? List.of()
                : verification.denominations().stream()
                .map(d -> new DenominationCountResponse(d.value(), d.quantity()))
                .toList();
        return new VerificationResponse(
                verification.id(),
                verification.budgetId(),
                budgetName,
                verification.verificationDate(),
                verification.note(),
                verification.referenceRest(),
                verification.countedTotal(),
                verification.difference(),
                denominations,
                verification.regularizationKind(),
                verification.regularizationPurchaseId(),
                verification.regularizationCreditMovementId(),
                verification.createdAt(),
                verification.createdBy()
        );
    }

    private String resolveBudgetName(java.util.UUID budgetId) {
        try {
            Budget budget = budgetService.getRawById(budgetId);
            return budget.name();
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }
}
