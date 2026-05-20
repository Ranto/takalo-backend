package ara.project.takalo.verification.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification_denominations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VerificationDenominationEntity {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private VerificationDenominationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("verificationId")
    @JoinColumn(name = "verification_id", nullable = false)
    private VerificationEntity verification;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
