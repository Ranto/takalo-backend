package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.application.port.out.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PermissionPersistenceAdapter implements PermissionRepository {

    private final JpaPermissionRepository jpaPermissionRepository;

    @Override
    public List<String> findAllNames() {
        return jpaPermissionRepository.findAllNames();
    }
}
