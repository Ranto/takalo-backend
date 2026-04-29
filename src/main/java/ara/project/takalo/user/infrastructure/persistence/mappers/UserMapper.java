package ara.project.takalo.user.infrastructure.persistence.mappers;

import ara.project.takalo.user.domain.model.User;
import ara.project.takalo.user.infrastructure.persistence.entities.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final RoleMapper roleMapper;

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return new User(
                entity.getId(),
                entity.getExternalId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getRoles().stream()
                        .map(roleMapper::toDomain)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
