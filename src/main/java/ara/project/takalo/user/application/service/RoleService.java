package ara.project.takalo.user.application.service;

import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.user.application.port.in.RoleServicePort;
import ara.project.takalo.user.application.port.out.RoleRepository;
import ara.project.takalo.user.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoleService implements RoleServicePort {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public Role getByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable: " + name));
    }
}
