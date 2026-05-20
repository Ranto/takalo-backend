package ara.project.takalo.verification.infrastructure.persistence.mappers;

import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationDenominationEntity;
import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationDenominationId;
import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VerificationMapper {

    public Verification toDomain(VerificationEntity entity) {
        if (entity == null) return null;
        List<DenominationCount> denominations = entity.getDenominations() == null
                ? List.of()
                : entity.getDenominations().stream()
                .map(d -> new DenominationCount(
                        d.getId() != null ? d.getId().getDenominationValue() : 0,
                        d.getQuantity()))
                .toList();

        return new Verification(
                entity.getId(),
                entity.getBudgetId(),
                entity.getOwnerId(),
                entity.getVerificationDate(),
                entity.getNote(),
                entity.getReferenceRest(),
                entity.getCountedTotal(),
                entity.getDifference(),
                denominations,
                entity.getRegularizationKind(),
                entity.getRegularizationPurchaseId(),
                entity.getRegularizationCreditMovementId(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    public VerificationEntity toEntity(Verification domain) {
        if (domain == null) return null;
        VerificationEntity entity = VerificationEntity.builder()
                .id(domain.id())
                .budgetId(domain.budgetId())
                .ownerId(domain.ownerId())
                .verificationDate(domain.verificationDate())
                .note(domain.note())
                .referenceRest(domain.referenceRest())
                .countedTotal(domain.countedTotal())
                .difference(domain.difference())
                .regularizationKind(domain.regularizationKind())
                .regularizationPurchaseId(domain.regularizationPurchaseId())
                .regularizationCreditMovementId(domain.regularizationCreditMovementId())
                .build();

        if (domain.denominations() != null) {
            domain.denominations().forEach(dc -> {
                VerificationDenominationEntity child = VerificationDenominationEntity.builder()
                        .id(new VerificationDenominationId(null, dc.value()))
                        .quantity(dc.quantity())
                        .build();
                entity.addDenomination(child);
            });
        }

        return entity;
    }
}
