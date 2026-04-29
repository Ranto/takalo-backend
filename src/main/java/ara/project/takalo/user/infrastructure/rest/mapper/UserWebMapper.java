package ara.project.takalo.user.infrastructure.rest.mapper;

import ara.project.takalo.user.domain.model.Permission;
import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.domain.model.User;
import ara.project.takalo.user.infrastructure.rest.dto.RoleResponse;
import ara.project.takalo.user.infrastructure.rest.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserWebMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(),
                user.externalId(),
                user.email(),
                user.displayName(),
                user.roleNames(),
                user.permissionNames(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.id(),
                role.name(),
                role.description(),
                role.permissions().stream().map(Permission::name).collect(Collectors.toUnmodifiableSet())
        );
    }
}
