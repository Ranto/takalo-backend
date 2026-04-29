package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseWebMapper {

    private final PurchaseItemWebMapper itemWebMapper;

    public Purchase toDomain(PurchaseRequest request, UUID effectiveBudgetId) {
        return new Purchase(
                null,
                null,
                effectiveBudgetId,
                request.purchaseDate(),
                request.items().stream().map(itemWebMapper::toDomain).toList()
        );
    }

    public Purchase toDomain(PurchaseRequest request) {
        JsonNullable<UUID> bid = request.budgetId();
        UUID effective = (bid == null || !bid.isPresent()) ? null : bid.get();
        return toDomain(request, effective);
    }

    public PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.id(),
                purchase.budgetId(),
                purchase.purchaseDate(),
                purchase.items().stream().map(itemWebMapper::toResponse).toList(),
                purchase.getTotalAmount());
    }
}
