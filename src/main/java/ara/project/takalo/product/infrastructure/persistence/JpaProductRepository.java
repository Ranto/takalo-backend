package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
                            WHERE (:categoryIds IS NULL OR p.categoryId IN :categoryIds)
                              AND (:name IS NULL OR p.name ILIKE %:name%)
                    """,
            countQuery = """
                    SELECT count(p) FROM ProductEntity p
                            WHERE (:categoryIds IS NULL OR p.categoryId IN :categoryIds)
                              AND (:name IS NULL OR p.name ILIKE %:name%)
                    """
    )
    Page<ProductEntity> findAllWithNameOrCategoryIdIn(@Param("name") String name, @Param("categoryIds") List<UUID> categoryIds, Pageable pageable);

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
}
