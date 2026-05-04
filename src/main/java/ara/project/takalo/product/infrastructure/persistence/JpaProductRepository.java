package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT p.id FROM ProductEntity p WHERE LOWER(p.name) = LOWER(:name)")
    Optional<UUID> findIdByName(@Param("name") String name);

    @Query(
            value = """
                    SELECT p FROM ProductEntity p
                            WHERE (:name IS NULL OR p.name ILIKE %:name%)
                              AND (
                                :hasCategoryFilter = false
                                OR (:includeUncategorized = true AND p.categoryId IS NULL)
                                OR (:categoryIds IS NOT NULL AND p.categoryId IN :categoryIds)
                              )
                    """,
            countQuery = """
                    SELECT count(p) FROM ProductEntity p
                            WHERE (:name IS NULL OR p.name ILIKE %:name%)
                              AND (
                                :hasCategoryFilter = false
                                OR (:includeUncategorized = true AND p.categoryId IS NULL)
                                OR (:categoryIds IS NOT NULL AND p.categoryId IN :categoryIds)
                              )
                    """
    )
    Page<ProductEntity> findAllWithNameOrCategoryIdIn(
            @Param("name") String name,
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("includeUncategorized") boolean includeUncategorized,
            @Param("hasCategoryFilter") boolean hasCategoryFilter,
            Pageable pageable);

    interface ProductIdAndName {
        UUID getId();
        String getName();
    }

    @Query("SELECT p.id as id, p.name as name FROM ProductEntity p WHERE p.id IN :ids")
    List<ProductIdAndName> findNamesById(@Param("ids") Set<UUID> ids);

    interface CategoryProductCount {
        UUID getCategoryId();
        long getCount();
    }

    @Query("SELECT p.categoryId as categoryId, COUNT(p.id) as count FROM ProductEntity p WHERE p.categoryId IN :ids GROUP BY p.categoryId")
    List<CategoryProductCount> countByCategoryIds(@Param("ids") Set<UUID> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.categoryId = :categoryId WHERE p.id IN :ids")
    int updateCategoryByIds(@Param("ids") List<UUID> ids, @Param("categoryId") UUID categoryId);
}
