package ara.project.takalo.purchase.application.port.in;

import ara.project.takalo.purchase.domain.model.Purchase;

import java.util.List;
import java.util.UUID;

public record BulkReassignBudgetResult(List<Purchase> purchases, UUID correlationId) {
}
