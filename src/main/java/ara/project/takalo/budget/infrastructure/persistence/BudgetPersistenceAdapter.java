package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEntity;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BudgetPersistenceAdapter implements BudgetRepository {

    private final JpaBudgetRepository jpaRepository;
    private final BudgetMapper mapper;

    @Override
    public Budget save(Budget budget) {
        BudgetEntity entity = mapper.toEntity(budget);
        BudgetEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public Map<UUID, BigDecimal> totalPurchasesByBudgetIds(Collection<UUID> budgetIds) {
        if (budgetIds == null || budgetIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.sumPurchasesByBudgetIds(budgetIds).stream()
                .collect(Collectors.toMap(
                        JpaBudgetRepository.BudgetTotalProjection::getBudgetId,
                        JpaBudgetRepository.BudgetTotalProjection::getTotal
                ));
    }

    @Override
    public PagedResponse<Budget> findAll(int page, int size) {
        Page<BudgetEntity> result = jpaRepository.findAll(PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(result, mapper::toDomain);
    }
}
