package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface JpaProductCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {
    Page<ProductCategoryEntity> findByLabelContainingIgnoreCase(String label, Pageable pageable);

    interface CategoryIdAndLabel {
        UUID getId();
        String getLabel();
    }

    @Query("SELECT c.id as id, c.label as label FROM ProductCategoryEntity c WHERE c.id IN :ids")
    List<CategoryIdAndLabel> findLabelsById(@Param("ids") Set<UUID> ids);
}
