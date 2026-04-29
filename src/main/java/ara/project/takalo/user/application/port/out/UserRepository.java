package ara.project.takalo.user.application.port.out;

import ara.project.takalo.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByExternalId(String externalId);

    User addRole(UUID userId, UUID roleId);

    User removeRole(UUID userId, UUID roleId);
}
