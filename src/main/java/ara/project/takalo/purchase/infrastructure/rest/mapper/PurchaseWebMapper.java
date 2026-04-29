package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseWebMapper {

    private final PurchaseItemWebMapper itemWebMapper;

    public Purchase toDomain(PurchaseRequest request) {
        return new Purchase(
                null,
                null,
                request.purchaseDate(),
                request.items().stream().map(itemWebMapper::toDomain).toList()
        );
    }

    public PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.id(),
                purchase.purchaseDate(),
                purchase.items().stream().map(itemWebMapper::toResponse).toList(),
                purchase.getTotalAmount());
    }
}
