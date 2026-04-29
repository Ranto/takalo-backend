package ara.project.takalo.user.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_default_budget")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDefaultBudgetEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "budget_id")
    private UUID budgetId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
