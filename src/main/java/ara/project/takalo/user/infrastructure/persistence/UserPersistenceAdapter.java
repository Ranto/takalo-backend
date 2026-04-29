package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.user.application.port.out.UserRepository;
import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.domain.model.User;
import ara.project.takalo.user.infrastructure.persistence.entities.RoleEntity;
import ara.project.takalo.user.infrastructure.persistence.entities.UserEntity;
import ara.project.takalo.user.infrastructure.persistence.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserEntity entity = jpaUserRepository.findByExternalId(user.externalId())
                .orElseGet(UserEntity::new);

        entity.setExternalId(user.externalId());
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());

        if (user.roles() != null && !user.roles().isEmpty()) {
            var roleNames = user.roles().stream().map(Role::name).collect(Collectors.toSet());
            var attached = new HashSet<RoleEntity>();
            for (String roleName : roleNames) {
                jpaRoleRepository.findByName(roleName).ifPresent(attached::add);
            }
            entity.setRoles(attached);
        }

        return userMapper.toDomain(jpaUserRepository.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByExternalId(String externalId) {
        return jpaUserRepository.findByExternalId(externalId).map(userMapper::toDomain);
    }

    @Override
    public User addRole(UUID userId, UUID roleId) {
        UserEntity user = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id: " + userId));
        RoleEntity role = jpaRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable avec id: " + roleId));
        user.getRoles().add(role);
        return userMapper.toDomain(jpaUserRepository.save(user));
    }

    @Override
    public User removeRole(UUID userId, UUID roleId) {
        UserEntity user = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id: " + userId));
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        return userMapper.toDomain(jpaUserRepository.save(user));
    }
}
