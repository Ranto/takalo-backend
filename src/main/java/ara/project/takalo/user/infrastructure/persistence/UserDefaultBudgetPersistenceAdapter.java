package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.application.port.out.UserDefaultBudgetRepository;
import ara.project.takalo.user.domain.model.UserDefaultBudget;
import ara.project.takalo.user.infrastructure.persistence.mappers.UserDefaultBudgetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDefaultBudgetPersistenceAdapter implements UserDefaultBudgetRepository {

    private final JpaUserDefaultBudgetRepository jpaRepository;
    private final UserDefaultBudgetMapper mapper;

    @Override
    public Optional<UserDefaultBudget> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public UserDefaultBudget save(UserDefaultBudget pref) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(pref)));
    }
}
