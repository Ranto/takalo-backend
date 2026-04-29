package ara.project.takalo.budget.infrastructure.rest.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record BudgetLightResponse(
        UUID id,
        String name,
        BigDecimal fond,
        BigDecimal reste,
        UUID createdBy,
        Set<UUID> editorIds
) {
}
