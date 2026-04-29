package ara.project.takalo.budget.infrastructure.rest.mapper;

import ara.project.takalo.budget.application.port.in.BudgetCreditCommand;
import ara.project.takalo.budget.application.port.in.BudgetTransferCommand;
import ara.project.takalo.budget.application.port.in.BudgetUpdateCommand;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetEditor;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetCreditRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetEditorResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetLightResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetMovementResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetTransferRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetUpdateRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class BudgetWebMapper {

    public Budget toDomain(BudgetRequest request) {
        return new Budget(
                null,
                request.name(),
                request.description(),
                request.fond(),
                null,
                null,
                null,
                null
        );
    }

    public BudgetUpdateCommand toCommand(BudgetUpdateRequest request) {
        return new BudgetUpdateCommand(
                request.name(),
                request.description(),
                Optional.ofNullable(request.fond())
        );
    }

    public BudgetResponse toResponse(BudgetWithBalance bwb) {
        Budget b = bwb.budget();
        BigDecimal fond = scale(b.initialFund());
        BigDecimal total = scale(bwb.totalPurchases());
        BigDecimal reste = scale(b.initialFund().subtract(bwb.totalPurchases()));
        return new BudgetResponse(
                b.id(),
                b.name(),
                b.description(),
                fond,
                total,
                reste,
                b.createdBy(),
                b.editorIds(),
                b.createdAt(),
                b.modifiedAt()
        );
    }

    public BudgetLightResponse toLightResponse(BudgetWithBalance bwb) {
        Budget b = bwb.budget();
        BigDecimal fond = scale(b.initialFund());
        BigDecimal reste = scale(b.initialFund().subtract(bwb.totalPurchases()));
        return new BudgetLightResponse(
                b.id(),
                b.name(),
                fond,
                reste,
                b.createdBy(),
                b.editorIds()
        );
    }

    public BudgetCreditCommand toCommand(BudgetCreditRequest request) {
        return new BudgetCreditCommand(request.montant(), request.date(), request.raison());
    }

    public BudgetTransferCommand toCommand(BudgetTransferRequest request) {
        return new BudgetTransferCommand(
                request.sourceBudgetId(),
                request.targetBudgetId(),
                request.montant(),
                request.date(),
                request.raison()
        );
    }

    public BudgetMovementResponse toMovementResponse(BudgetMovement m) {
        return new BudgetMovementResponse(
                m.id(),
                m.type(),
                scale(m.amount()),
                m.occurredAt(),
                m.reason(),
                m.correlationId(),
                m.source(),
                m.counterpartBudgetId()
        );
    }

    public BudgetEditorResponse toEditorResponse(BudgetEditor editor) {
        return new BudgetEditorResponse(editor.userId(), editor.displayName(), editor.creator());
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
