package ara.project.takalo.purchase.application.port.in;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public record CategoryBreakdownQuery(Instant start,
                                     Instant end,
                                     Collection<UUID> budgetIds,
                                     boolean includeUnbudgeted) {
}
