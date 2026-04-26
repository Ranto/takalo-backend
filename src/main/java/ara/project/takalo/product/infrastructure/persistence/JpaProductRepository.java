package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    @Query(
            value = """
                    SELECT p FROM ProductEntity p
                            JOIN FETCH p.category
                            WHERE (:categoryIds IS NULL OR p.category.id IN :categoryIds)
                              AND (:name IS NULL OR p.name ILIKE %:name%)
                    """,
            countQuery = """
                    SELECT count(p) FROM ProductEntity p
                            WHERE (:categoryIds IS NULL OR p.category.id IN :categoryIds)
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
}
