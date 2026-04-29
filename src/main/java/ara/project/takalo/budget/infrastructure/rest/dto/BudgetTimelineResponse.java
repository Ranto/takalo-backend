package ara.project.takalo.budget.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetTimelineResponse(
        List<BudgetTimelineEventResponse> events,
        BigDecimal restRefStart,
        BigDecimal restRefEnd
) {
}
