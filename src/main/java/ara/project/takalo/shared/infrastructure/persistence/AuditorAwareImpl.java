package ara.project.takalo.shared.infrastructure.persistence;

import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("auditorAware")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<UUID> {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return currentUserProvider.current().map(u -> u.id());
    }
}
