package ara.project.takalo.user.application.service;

import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.user.application.port.out.RoleRepository;
import ara.project.takalo.user.application.port.out.UserRepository;
import ara.project.takalo.user.domain.model.Role;
import ara.project.takalo.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService service;

    @Test
    void revokeRole_whenUserKeepsAtLeastOneRole_removesIt() {
        UUID userId = UUID.randomUUID();
        Role userRole = new Role(UUID.randomUUID(), "USER", null, Set.of());
        Role adminRole = new Role(UUID.randomUUID(), "ADMIN", null, Set.of());
        User user = new User(userId, "ext", "a@b", "Alice",
                Set.of(userRole, adminRole), Instant.now(), Instant.now());

        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.removeRole(userId, adminRole.id())).thenReturn(user);

        service.revokeRole(userId, "ADMIN");

        verify(userRepository).removeRole(userId, adminRole.id());
    }

    @Test
    void revokeRole_whenLastRole_throwsInvalidOperation() {
        UUID userId = UUID.randomUUID();
        Role userRole = new Role(UUID.randomUUID(), "USER", null, Set.of());
        User user = new User(userId, "ext", "a@b", "Alice",
                Set.of(userRole), Instant.now(), Instant.now());

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.revokeRole(userId, "USER"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("au moins un rôle");

        verify(userRepository, never()).removeRole(any(), any());
    }

    @Test
    void revokeRole_whenRoleNotHeld_doesNotBlockOnSingleRoleRule() {
        UUID userId = UUID.randomUUID();
        Role userRole = new Role(UUID.randomUUID(), "USER", null, Set.of());
        Role adminRole = new Role(UUID.randomUUID(), "ADMIN", null, Set.of());
        User user = new User(userId, "ext", "a@b", "Alice",
                Set.of(userRole), Instant.now(), Instant.now());

        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.removeRole(userId, adminRole.id())).thenReturn(user);

        service.revokeRole(userId, "ADMIN");

        verify(userRepository).removeRole(userId, adminRole.id());
    }
}
