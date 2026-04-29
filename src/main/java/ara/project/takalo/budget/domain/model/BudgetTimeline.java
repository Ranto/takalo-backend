package ara.project.takalo.budget.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record BudgetTimeline(
        List<BudgetTimelineEvent> events,
        BigDecimal restRefStart,
        BigDecimal restRefEnd
) {
}
