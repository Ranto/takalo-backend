package ara.project.takalo.user.application.port.in;

import ara.project.takalo.user.domain.model.Role;

import java.util.List;

public interface RoleServicePort {

    List<Role> findAll();

    Role getByName(String name);
}
