package ara.project.takalo.user.application.service;

import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.application.port.out.RoleRepository;
import ara.project.takalo.user.application.port.out.UserRepository;
import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserServicePort {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    @Override
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public User getByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId);
    }

    @Override
    public User provision(String externalId, String email, String displayName) {
        Optional<User> byExternalId = userRepository.findByExternalId(externalId);
        if (byExternalId.isPresent()) {
            return byExternalId.get();
        }
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            User linked = new User(
                    existing.id(),
                    externalId,
                    existing.email(),
                    existing.displayName(),
                    existing.roles(),
                    existing.createdAt(),
                    existing.updatedAt());
            return userRepository.save(linked);
        }
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle par défaut introuvable: " + DEFAULT_ROLE));
        User toCreate = new User(
                null,
                externalId,
                email,
                displayName,
                Set.of(defaultRole),
                null,
                null);
        return userRepository.save(toCreate);
    }

    @Override
    public User assignRole(UUID userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable: " + roleName));
        return userRepository.addRole(userId, role.id());
    }

    @Override
    public User revokeRole(UUID userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable: " + roleName));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id: " + userId));
        if (user.roles().stream().anyMatch(r -> r.id().equals(role.id())) && user.roles().size() <= 1) {
            throw new InvalidOperationException("Un utilisateur doit conserver au moins un rôle");
        }
        return userRepository.removeRole(userId, role.id());
    }
}
