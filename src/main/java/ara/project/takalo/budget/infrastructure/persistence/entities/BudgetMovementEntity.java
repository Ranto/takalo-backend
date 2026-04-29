package ara.project.takalo.budget.infrastructure.persistence.entities;

import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "budget_movements",
        indexes = {
                @Index(name = "idx_budget_movements_budget", columnList = "budget_id, occurred_at"),
                @Index(name = "idx_budget_movements_correlation", columnList = "correlation_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BudgetMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BudgetMovementType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 40)
    private BudgetMovementSource source;

    @Column(name = "counterpart_budget_id")
    private UUID counterpartBudgetId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
