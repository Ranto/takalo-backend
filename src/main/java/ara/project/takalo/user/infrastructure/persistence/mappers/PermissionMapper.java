package ara.project.takalo.user.infrastructure.persistence.mappers;

import ara.project.takalo.user.domain.model.Permission;
import ara.project.takalo.user.infrastructure.persistence.entities.PermissionEntity;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {
    public Permission toDomain(PermissionEntity entity) {
        if (entity == null) return null;
        return new Permission(entity.getId(), entity.getName(), entity.getDescription());
    }
}
