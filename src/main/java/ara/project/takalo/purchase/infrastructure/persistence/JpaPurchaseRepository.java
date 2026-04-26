package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface JpaPurchaseRepository extends JpaRepository<PurchaseEntity, UUID> {

    @Query(
            value = """
        SELECT p FROM PurchaseEntity p
        WHERE (:startDate IS NULL OR p.purchaseDate >= :startDate)
          AND (:endDate IS NULL OR p.purchaseDate <= :endDate)
        ORDER BY p.purchaseDate DESC
        """,
            countQuery = """
        SELECT COUNT(p) FROM PurchaseEntity p
        WHERE (:startDate IS NULL OR p.purchaseDate >= :startDate)
          AND (:endDate IS NULL OR p.purchaseDate <= :endDate)
        """
    )
    @EntityGraph(attributePaths = "items")
    Page<PurchaseEntity> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
}
