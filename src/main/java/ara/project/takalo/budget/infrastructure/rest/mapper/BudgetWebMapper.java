package ara.project.takalo.budget.infrastructure.rest.mapper;

import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetLightResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    private BigDecimal scale(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
