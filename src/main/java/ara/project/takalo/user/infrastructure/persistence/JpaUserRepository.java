package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByExternalId(String externalId);

    Optional<UserEntity> findByEmail(String email);

    @Query(
            value = """
                    SELECT u FROM UserEntity u
                            WHERE :query IS NULL
                               OR u.email ILIKE %:query%
                               OR u.displayName ILIKE %:query%
                    """,
            countQuery = """
                    SELECT count(u) FROM UserEntity u
                            WHERE :query IS NULL
                               OR u.email ILIKE %:query%
                               OR u.displayName ILIKE %:query%
                    """
    )
    Page<UserEntity> search(@Param("query") String query, Pageable pageable);
}
