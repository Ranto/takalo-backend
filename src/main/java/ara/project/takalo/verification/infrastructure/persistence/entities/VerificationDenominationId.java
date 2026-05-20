package ara.project.takalo.verification.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VerificationDenominationId implements Serializable {

    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(name = "denomination_value", nullable = false)
    private int denominationValue;
}
