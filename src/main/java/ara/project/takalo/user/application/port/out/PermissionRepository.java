package ara.project.takalo.user.application.port.out;

import java.util.List;

public interface PermissionRepository {
    List<String> findAllNames();
}
