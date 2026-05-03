package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.application.port.in.CategoryBreakdownQuery;
import ara.project.takalo.purchase.application.port.in.PurchaseItemDetailQuery;
import ara.project.takalo.purchase.application.port.out.PurchaseRepository;
import ara.project.takalo.purchase.domain.model.CategorySpendingBreakdown;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseMapper;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.utility.PaginationMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PurchasePersistenceAdapter implements PurchaseRepository {

    private final JpaPurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Purchase save(Purchase purchase) {
        PurchaseEntity entity = purchaseMapper.toEntity(purchase);
        PurchaseEntity entitySave = purchaseRepository.save(entity);
        return purchaseMapper.toDomain(entitySave);
    }

    @Override
    public Optional<Purchase> findById(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId).map(purchaseMapper::toDomain);
    }

    @Override
    public PagedResponse<Purchase> findAll(int page, int size) {
        Page<PurchaseEntity> resultPage = purchaseRepository.findAll(PageRequest.of(page, size));
        return PaginationMapper.toPagedResponse(resultPage, purchaseMapper::toDomain);
    }

    @Override
    public PagedResponse<Purchase> findByDateRange(Instant start, Instant end, Boolean locked,
                                                   UUID budgetId, boolean includeUnbudgeted,
                                                   int page, int size) {
        var pageable = PageRequest.of(page, size);
        boolean hasBudgetFilter = budgetId != null || includeUnbudgeted;
        var entityPage = purchaseRepository.findByDateRange(
                start, end, lockedFilter(locked), budgetId, includeUnbudgeted, hasBudgetFilter, pageable);

        return PaginationMapper.toPagedResponse(entityPage, purchaseMapper::toDomain);
    }

    @Override
    public PagedResponse<Purchase> findByDateRangeAndOwner(Instant start, Instant end, UUID ownerId,
                                                           Boolean locked, UUID budgetId, boolean includeUnbudgeted,
                                                           int page, int size) {
        var pageable = PageRequest.of(page, size);
        boolean hasBudgetFilter = budgetId != null || includeUnbudgeted;
        var entityPage = purchaseRepository.findByDateRangeAndOwner(
                start, end, ownerId, lockedFilter(locked), budgetId, includeUnbudgeted, hasBudgetFilter, pageable);

        return PaginationMapper.toPagedResponse(entityPage, purchaseMapper::toDomain);
    }

    private static String lockedFilter(Boolean locked) {
        if (locked == null) return "ALL";
        return locked ? "LOCKED" : "UNLOCKED";
    }

    @Override
    public List<Purchase> findByBudgetId(UUID budgetId) {
        return purchaseRepository.findByBudgetId(budgetId).stream()
                .map(purchaseMapper::toDomain)
                .toList();
    }

    @Override
    public List<Purchase> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return purchaseRepository.findByIdIn(ids).stream()
                .map(purchaseMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID purchaseId) {
        if (!purchaseRepository.existsById(purchaseId)) {
            throw new ResourceNotFoundException("Achat non trouvé");
        }
        purchaseRepository.deleteById(purchaseId);
    }

    @Override
    public PagedResponse<PurchaseItemDetail> searchItemDetails(PurchaseItemDetailQuery query, UUID ownerId) {
        String orderByExpr = switch (query.sort()) {
            case DATE -> "i.purchase.purchaseDate";
            case PRODUCT -> "LOWER(i.productName)";
            case CATEGORY -> "LOWER(cat.label)";
        };
        String direction = query.direction() == PurchaseItemDetailQuery.SortDirection.ASC ? "ASC" : "DESC";

        String fromAndJoins = """
                FROM PurchaseItemEntity i
                LEFT JOIN ProductEntity prod ON prod.id = i.productId
                LEFT JOIN ProductCategoryEntity cat ON cat.id = prod.categoryId
                """;

        // WHERE dynamique : on n'ajoute un prédicat que si la valeur est présente.
        // Évite les patterns `:p IS NULL OR ...` qui font échouer PostgreSQL sur les
        // paramètres null non typés (« could not determine data type of parameter »).
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (query.start() != null) {
            where.append(" AND i.purchase.purchaseDate >= :start");
            params.put("start", query.start());
        }
        if (query.end() != null) {
            where.append(" AND i.purchase.purchaseDate <= :end");
            params.put("end", query.end());
        }
        if (ownerId != null) {
            where.append(" AND i.purchase.ownerId = :ownerId");
            params.put("ownerId", ownerId);
        }
        String productName = emptyToNull(query.productName());
        if (productName != null) {
            where.append(" AND LOWER(i.productName) LIKE LOWER(CONCAT('%', :productName, '%'))");
            params.put("productName", productName);
        }
        String categoryName = emptyToNull(query.categoryName());
        if (categoryName != null) {
            where.append(" AND LOWER(cat.label) LIKE LOWER(CONCAT('%', :categoryName, '%'))");
            params.put("categoryName", categoryName);
        }
        if (query.budgetId() != null) {
            where.append(" AND i.purchase.budgetId = :budgetId");
            params.put("budgetId", query.budgetId());
        }
        if (query.productId() != null) {
            where.append(" AND i.productId = :productId");
            params.put("productId", query.productId());
        }
        if (query.categoryId() != null) {
            where.append(" AND prod.categoryId = :categoryId");
            params.put("categoryId", query.categoryId());
        }

        String selectJpql = "SELECT new ara.project.takalo.purchase.domain.model.PurchaseItemDetail("
                + "i.id, i.purchase.id, i.purchase.purchaseDate, "
                + "i.productId, i.productName, "
                + "prod.categoryId, cat.label, "
                + "i.unitPrice, i.quantity, i.discount, i.storeName) "
                + fromAndJoins + where
                + " ORDER BY " + orderByExpr + " " + direction + ", i.id ASC";

        String countJpql = "SELECT COUNT(i) " + fromAndJoins + where;

        TypedQuery<PurchaseItemDetail> selectQuery = entityManager.createQuery(selectJpql, PurchaseItemDetail.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        params.forEach((k, v) -> {
            selectQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        selectQuery.setFirstResult(query.page() * query.size());
        selectQuery.setMaxResults(query.size());

        List<PurchaseItemDetail> content = selectQuery.getResultList();
        long total = countQuery.getSingleResult();
        int totalPages = query.size() == 0 ? 0 : (int) ((total + query.size() - 1) / query.size());
        boolean isLast = (long) (query.page() + 1) * query.size() >= total;
        return new PagedResponse<>(content, query.page(), query.size(), total, totalPages, isLast);
    }

    @Override
    public List<CategorySpendingBreakdown> categoryBreakdown(CategoryBreakdownQuery query, UUID ownerId) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (query.start() != null) {
            where.append(" AND i.purchase.purchaseDate >= :start");
            params.put("start", query.start());
        }
        if (query.end() != null) {
            where.append(" AND i.purchase.purchaseDate <= :end");
            params.put("end", query.end());
        }
        if (ownerId != null) {
            where.append(" AND i.purchase.ownerId = :ownerId");
            params.put("ownerId", ownerId);
        }

        Collection<UUID> budgetIds = query.budgetIds();
        boolean hasBudgetIds = budgetIds != null && !budgetIds.isEmpty();
        boolean includeUnbudgeted = query.includeUnbudgeted();
        if (hasBudgetIds && includeUnbudgeted) {
            where.append(" AND (i.purchase.budgetId IN :budgetIds OR i.purchase.budgetId IS NULL)");
            params.put("budgetIds", budgetIds);
        } else if (hasBudgetIds) {
            where.append(" AND i.purchase.budgetId IN :budgetIds");
            params.put("budgetIds", budgetIds);
        } else if (includeUnbudgeted) {
            where.append(" AND i.purchase.budgetId IS NULL");
        }

        String jpql = "SELECT prod.categoryId, cat.label, "
                + "SUM(i.unitPrice * i.quantity - COALESCE(i.discount, 0)), "
                + "COUNT(i) "
                + "FROM PurchaseItemEntity i "
                + "LEFT JOIN ProductEntity prod ON prod.id = i.productId "
                + "LEFT JOIN ProductCategoryEntity cat ON cat.id = prod.categoryId"
                + where
                + " GROUP BY prod.categoryId, cat.label "
                + "ORDER BY SUM(i.unitPrice * i.quantity - COALESCE(i.discount, 0)) DESC";

        TypedQuery<Object[]> q = entityManager.createQuery(jpql, Object[].class);
        params.forEach(q::setParameter);
        return q.getResultList().stream()
                .map(row -> new CategorySpendingBreakdown(
                        (UUID) row[0],
                        (String) row[1],
                        toBigDecimal(row[2]),
                        ((Number) row[3]).longValue()))
                .toList();
    }

    private static java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) return java.math.BigDecimal.ZERO;
        if (value instanceof java.math.BigDecimal bd) return bd;
        if (value instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        return new java.math.BigDecimal(value.toString());
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
