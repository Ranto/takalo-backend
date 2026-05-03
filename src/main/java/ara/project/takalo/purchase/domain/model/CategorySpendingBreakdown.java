package ara.project.takalo.purchase.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendingBreakdown(UUID categoryId,
                                        String categoryLabel,
                                        BigDecimal total,
                                        Long itemCount) {
}
