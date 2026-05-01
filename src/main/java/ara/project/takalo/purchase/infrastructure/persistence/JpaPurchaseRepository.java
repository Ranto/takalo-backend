package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaPurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    List<PurchaseEntity> findByBudgetId(UUID budgetId);

    @EntityGraph(attributePaths = "items")
    List<PurchaseEntity> findByIdIn(Collection<UUID> ids);


    @Query(
            value = """
        SELECT p FROM PurchaseEntity p
        WHERE p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
        ORDER BY p.purchaseDate DESC
        """,
            countQuery = """
        SELECT COUNT(p) FROM PurchaseEntity p
        WHERE p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
        """
    )
    @EntityGraph(attributePaths = "items")
    Page<PurchaseEntity> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT p FROM PurchaseEntity p
        WHERE p.ownerId = :ownerId
          AND p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
        ORDER BY p.purchaseDate DESC
        """,
            countQuery = """
        SELECT COUNT(p) FROM PurchaseEntity p
        WHERE p.ownerId = :ownerId
          AND p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
        """
    )
    @EntityGraph(attributePaths = "items")
    Page<PurchaseEntity> findByDateRangeAndOwner(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("ownerId") UUID ownerId,
            Pageable pageable
    );
}
