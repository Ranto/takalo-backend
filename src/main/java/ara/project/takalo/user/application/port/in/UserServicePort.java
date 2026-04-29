package ara.project.takalo.user.application.port.in;

import ara.project.takalo.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserServicePort {

    User getById(UUID id);

    User getByExternalId(String externalId);

    Optional<User> findByExternalId(String externalId);

    User provision(String externalId, String email, String displayName);

    User assignRole(UUID userId, String roleName);

    User revokeRole(UUID userId, String roleName);
}
