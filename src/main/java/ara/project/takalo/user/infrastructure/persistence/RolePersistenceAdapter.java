package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.application.port.out.RoleRepository;
import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.infrastructure.persistence.mappers.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RolePersistenceAdapter implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;
    private final RoleMapper roleMapper;

    @Override
    public List<Role> findAll() {
        return jpaRoleRepository.findAll().stream().map(roleMapper::toDomain).toList();
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRoleRepository.findByName(name).map(roleMapper::toDomain);
    }
}
