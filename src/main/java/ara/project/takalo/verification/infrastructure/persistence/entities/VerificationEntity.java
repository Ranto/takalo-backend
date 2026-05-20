package ara.project.takalo.verification.infrastructure.persistence.entities;

import ara.project.takalo.verification.domain.model.RegularizationKind;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "verifications",
        indexes = {
                @Index(name = "idx_verifications_budget_date", columnList = "budget_id, verification_date DESC"),
                @Index(name = "idx_verifications_owner", columnList = "owner_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class VerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "verification_date", nullable = false)
    private LocalDate verificationDate;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "reference_rest", precision = 15, scale = 2, nullable = false)
    private BigDecimal referenceRest;

    @Column(name = "counted_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal countedTotal;

    @Column(name = "difference", precision = 15, scale = 2, nullable = false)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "regularization_kind", length = 16, nullable = false)
    private RegularizationKind regularizationKind;

    @Column(name = "regularization_purchase_id")
    private UUID regularizationPurchaseId;

    @Column(name = "regularization_credit_movement_id")
    private UUID regularizationCreditMovementId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "modified_by")
    private UUID modifiedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @OneToMany(
            mappedBy = "verification",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<VerificationDenominationEntity> denominations = new ArrayList<>();

    public void addDenomination(VerificationDenominationEntity denomination) {
        if (denominations == null) {
            denominations = new ArrayList<>();
        }
        denominations.add(denomination);
        denomination.setVerification(this);
    }
}
