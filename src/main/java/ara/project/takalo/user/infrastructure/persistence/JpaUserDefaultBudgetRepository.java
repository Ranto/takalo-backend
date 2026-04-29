package ara.project.takalo.user.infrastructure.persistence;

import ara.project.takalo.user.infrastructure.persistence.entities.UserDefaultBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserDefaultBudgetRepository extends JpaRepository<UserDefaultBudgetEntity, UUID> {
}
