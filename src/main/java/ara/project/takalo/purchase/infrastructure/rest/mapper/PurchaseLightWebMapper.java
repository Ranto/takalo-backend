package ara.project.takalo.purchase.infrastructure.rest.mapper;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseLightResponse;
import org.springframework.stereotype.Component;

@Component
public class PurchaseLightWebMapper {

    public PurchaseLightResponse toResponse(Purchase domain) {
        return new PurchaseLightResponse(
                domain.id(),
                domain.budgetId(),
                domain.purchaseDate(),
                domain.items().size(),
                domain.getTotalAmount()
        );
    }
}
