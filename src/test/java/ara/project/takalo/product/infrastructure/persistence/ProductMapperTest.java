package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import ara.project.takalo.product.infrastructure.persistence.mappers.ProductMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-02-01T00:00:00Z");
        Product domain = new Product(id, "Phone", categoryId, created, updated);

        ProductEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getName()).isEqualTo("Phone");
        assertThat(entity.getCategoryId()).isEqualTo(categoryId);
        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void toEntity_withNullId_buildsEntityWithNullId() {
        Product domain = new Product(null, "Phone", null, null, null);

        ProductEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Phone");
        assertThat(entity.getCategoryId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_withNull() {
        ProductEntity entity = mapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-02-01T00:00:00Z");
        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .name("Phone")
                .categoryId(categoryId)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        Product domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.name()).isEqualTo("Phone");
        assertThat(domain.categoryId()).isEqualTo(categoryId);
        assertThat(domain.createdAt()).isEqualTo(created);
        assertThat(domain.updatedAt()).isEqualTo(updated);
    }

    @Test
    void toDomain_null() {
        Product domain = mapper.toDomain(null);

        assertThat(domain).isNull();
    }
}
