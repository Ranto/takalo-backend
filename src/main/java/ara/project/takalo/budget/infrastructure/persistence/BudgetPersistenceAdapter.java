package ara.project.takalo.budget.infrastructure.persistence;

import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEditorEntity;
import ara.project.takalo.budget.infrastructure.persistence.entities.BudgetEntity;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BudgetPersistenceAdapter implements BudgetRepository {

    private final JpaBudgetRepository jpaRepository;
    private final BudgetMapper mapper;

    @Override
    public Budget save(Budget budget) {
        BudgetEntity entity;
        if (budget.id() != null) {
            entity = jpaRepository.findById(budget.id())
                    .orElseGet(() -> mapper.toEntity(budget));
            entity.setName(budget.name());
            entity.setDescription(budget.description());
            entity.setInitialFund(budget.initialFund());
            syncEditors(entity, budget);
        } else {
            entity = mapper.toEntity(budget);
        }
        BudgetEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    private void syncEditors(BudgetEntity entity, Budget budget) {
        Set<UUID> desired = budget.editorIds() == null ? Set.of() : budget.editorIds();
        Set<BudgetEditorEntity> editors = entity.getEditors();
        if (editors == null) {
            editors = new HashSet<>();
            entity.setEditors(editors);
        }
        editors.removeIf(e -> !desired.contains(e.getUserId()));
        Set<UUID> existing = editors.stream()
                .map(BudgetEditorEntity::getUserId)
                .collect(Collectors.toSet());
        Instant now = Instant.now();
        for (UUID uid : desired) {
            if (!existing.contains(uid)) {
                editors.add(BudgetEditorEntity.builder()
                        .userId(uid)
                        .addedAt(now)
                        .build());
            }
        }
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
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
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public PagedResponse<Budget> findAll(int page, int size) {
        Page<BudgetEntity> result = jpaRepository.findAll(PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(result, mapper::toDomain);
    }
}
