package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.infrastructure.persistence.entities.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    @Query("select p.name from PermissionEntity p")
    List<String> findAllNames();
}
