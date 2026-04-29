package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaBudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    boolean existsByName(String name);

    @Query("""
            SELECT pi.purchase.budgetId AS budgetId,
                   COALESCE(SUM(pi.unitPrice * pi.quantity - pi.discount), 0) AS total
            FROM PurchaseItemEntity pi
            WHERE pi.purchase.budgetId IN :budgetIds
            GROUP BY pi.purchase.budgetId
            """)
    List<BudgetTotalProjection> sumPurchasesByBudgetIds(@Param("budgetIds") Collection<UUID> budgetIds);

    interface BudgetTotalProjection {
        UUID getBudgetId();

        java.math.BigDecimal getTotal();
    }
}
