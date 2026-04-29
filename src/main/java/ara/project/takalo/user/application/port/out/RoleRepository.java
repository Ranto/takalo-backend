package ara.project.takalo.user.application.port.out;

import ara.project.takalo.user.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    List<Role> findAll();

    Optional<Role> findByName(String name);
}
