package ara.project.takalo.verification.infrastructure.persistence;

import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.RegularizationKind;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationEntity;
import ara.project.takalo.verification.infrastructure.persistence.mappers.VerificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationMapperTest {

    private VerificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VerificationMapper();
    }

    @Test
    void toEntity_thenToDomain_preservesFields() {
        UUID id = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Verification domain = new Verification(
                id, budgetId, ownerId,
                LocalDate.of(2026, 5, 20),
                "test note",
                new BigDecimal("100.00"),
                new BigDecimal("95.00"),
                new BigDecimal("-5.00"),
                List.of(new DenominationCount(10000, 1), new DenominationCount(500, 2)),
                RegularizationKind.PURCHASE,
                UUID.randomUUID(),
                null,
                null,
                null
        );

        VerificationEntity entity = mapper.toEntity(domain);
        assertThat(entity.getDenominations()).hasSize(2);
        assertThat(entity.getRegularizationKind()).isEqualTo(RegularizationKind.PURCHASE);

        // Round-trip: simulate id propagated through @MapsId by setting denom ids
        entity.getDenominations().forEach(d -> {
            d.getId().setVerificationId(id);
        });
        Verification back = mapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(id);
        assertThat(back.budgetId()).isEqualTo(budgetId);
        assertThat(back.note()).isEqualTo("test note");
        assertThat(back.difference()).isEqualByComparingTo("-5.00");
        assertThat(back.denominations()).extracting(DenominationCount::value)
                .containsExactly(10000, 500);
        assertThat(back.denominations()).extracting(DenominationCount::quantity)
                .containsExactly(1, 2);
        assertThat(back.regularizationKind()).isEqualTo(RegularizationKind.PURCHASE);
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
