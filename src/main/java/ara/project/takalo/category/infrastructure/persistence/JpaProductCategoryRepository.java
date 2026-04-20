package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaProductCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {
    Page<ProductCategoryEntity> findByLabelContainingIgnoreCase(String label, Pageable pageable);
}
