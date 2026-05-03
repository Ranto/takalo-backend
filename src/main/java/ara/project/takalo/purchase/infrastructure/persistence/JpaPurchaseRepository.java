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
          AND (:lockedFilter = 'ALL'
               OR (:lockedFilter = 'LOCKED' AND p.lockedAt IS NOT NULL)
               OR (:lockedFilter = 'UNLOCKED' AND p.lockedAt IS NULL))
          AND (
            :hasBudgetFilter = false
            OR (:includeUnbudgeted = true AND p.budgetId IS NULL)
            OR (:budgetId IS NOT NULL AND p.budgetId = :budgetId)
          )
        ORDER BY p.purchaseDate DESC
        """,
            countQuery = """
        SELECT COUNT(p) FROM PurchaseEntity p
        WHERE p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
          AND (:lockedFilter = 'ALL'
               OR (:lockedFilter = 'LOCKED' AND p.lockedAt IS NOT NULL)
               OR (:lockedFilter = 'UNLOCKED' AND p.lockedAt IS NULL))
          AND (
            :hasBudgetFilter = false
            OR (:includeUnbudgeted = true AND p.budgetId IS NULL)
            OR (:budgetId IS NOT NULL AND p.budgetId = :budgetId)
          )
        """
    )
    @EntityGraph(attributePaths = "items")
    Page<PurchaseEntity> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("lockedFilter") String lockedFilter,
            @Param("budgetId") UUID budgetId,
            @Param("includeUnbudgeted") boolean includeUnbudgeted,
            @Param("hasBudgetFilter") boolean hasBudgetFilter,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT p FROM PurchaseEntity p
        WHERE p.ownerId = :ownerId
          AND p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
          AND (:lockedFilter = 'ALL'
               OR (:lockedFilter = 'LOCKED' AND p.lockedAt IS NOT NULL)
               OR (:lockedFilter = 'UNLOCKED' AND p.lockedAt IS NULL))
          AND (
            :hasBudgetFilter = false
            OR (:includeUnbudgeted = true AND p.budgetId IS NULL)
            OR (:budgetId IS NOT NULL AND p.budgetId = :budgetId)
          )
        ORDER BY p.purchaseDate DESC
        """,
            countQuery = """
        SELECT COUNT(p) FROM PurchaseEntity p
        WHERE p.ownerId = :ownerId
          AND p.purchaseDate >= COALESCE(:startDate, p.purchaseDate)
          AND p.purchaseDate <= COALESCE(:endDate, p.purchaseDate)
          AND (:lockedFilter = 'ALL'
               OR (:lockedFilter = 'LOCKED' AND p.lockedAt IS NOT NULL)
               OR (:lockedFilter = 'UNLOCKED' AND p.lockedAt IS NULL))
          AND (
            :hasBudgetFilter = false
            OR (:includeUnbudgeted = true AND p.budgetId IS NULL)
            OR (:budgetId IS NOT NULL AND p.budgetId = :budgetId)
          )
        """
    )
    @EntityGraph(attributePaths = "items")
    Page<PurchaseEntity> findByDateRangeAndOwner(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("ownerId") UUID ownerId,
            @Param("lockedFilter") String lockedFilter,
            @Param("budgetId") UUID budgetId,
            @Param("includeUnbudgeted") boolean includeUnbudgeted,
            @Param("hasBudgetFilter") boolean hasBudgetFilter,
            Pageable pageable
    );
}
