package ara.project.takalo.user.infrastructure.persistence.mappers;

import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.infrastructure.persistence.entities.RoleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public Role toDomain(RoleEntity entity) {
        if (entity == null) return null;
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPermissions().stream()
                        .map(permissionMapper::toDomain)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
