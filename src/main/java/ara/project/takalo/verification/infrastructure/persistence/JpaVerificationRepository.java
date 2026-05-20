package ara.project.takalo.verification.infrastructure.persistence;

import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface JpaVerificationRepository extends JpaRepository<VerificationEntity, UUID> {

    @Query(
            value = """
            SELECT v FROM VerificationEntity v
            WHERE (:budgetId IS NULL OR v.budgetId = :budgetId)
              AND (:ownerFilter IS NULL OR v.ownerId = :ownerFilter)
              AND v.verificationDate >= COALESCE(:startDate, v.verificationDate)
              AND v.verificationDate <= COALESCE(:endDate, v.verificationDate)
            ORDER BY v.verificationDate DESC, v.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(v) FROM VerificationEntity v
            WHERE (:budgetId IS NULL OR v.budgetId = :budgetId)
              AND (:ownerFilter IS NULL OR v.ownerId = :ownerFilter)
              AND v.verificationDate >= COALESCE(:startDate, v.verificationDate)
              AND v.verificationDate <= COALESCE(:endDate, v.verificationDate)
            """
    )
    @EntityGraph(attributePaths = "denominations")
    Page<VerificationEntity> search(
            @Param("budgetId") UUID budgetId,
            @Param("ownerFilter") UUID ownerFilter,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
