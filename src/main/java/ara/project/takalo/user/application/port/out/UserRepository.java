package ara.project.takalo.user.application.port.out;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByExternalId(String externalId);

    Optional<User> findByEmail(String email);

    PagedResponse<User> search(String query, int page, int size);

    User addRole(UUID userId, UUID roleId);

    User removeRole(UUID userId, UUID roleId);
}
