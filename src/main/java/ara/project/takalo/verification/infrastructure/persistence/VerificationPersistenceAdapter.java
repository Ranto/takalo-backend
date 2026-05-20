package ara.project.takalo.verification.infrastructure.persistence;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import ara.project.takalo.verification.application.port.out.VerificationRepository;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.persistence.entities.VerificationEntity;
import ara.project.takalo.verification.infrastructure.persistence.mappers.VerificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VerificationPersistenceAdapter implements VerificationRepository {

    private final JpaVerificationRepository jpaRepository;
    private final VerificationMapper mapper;

    @Override
    public Verification save(Verification verification) {
        VerificationEntity entity = mapper.toEntity(verification);
        VerificationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Verification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResponse<Verification> search(UUID budgetId, LocalDate start, LocalDate end,
                                              UUID ownerFilter, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entityPage = jpaRepository.search(budgetId, ownerFilter, start, end, pageable);
        return PaginationMapper.toPagedResponse(entityPage, mapper::toDomain);
    }
}
