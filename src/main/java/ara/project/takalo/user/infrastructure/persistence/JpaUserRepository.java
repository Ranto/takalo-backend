package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.infrastructure.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByExternalId(String externalId);
}
