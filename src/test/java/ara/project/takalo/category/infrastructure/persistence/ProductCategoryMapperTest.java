package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.category.infrastructure.persistence.mappers.ProductCategoryMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCategoryMapperTest {

    private final ProductCategoryMapper mapper = new ProductCategoryMapper();

    @Test
    void toEntity_mapsAllFields() {
        UUID id = UUID.randomUUID();
        ProductCategory domain = new ProductCategory(id, "Books", "All books");

        ProductCategoryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getLabel()).isEqualTo("Books");
        assertThat(entity.getDescription()).isEqualTo("All books");
    }

    @Test
    void toEntity_withNullId_buildsEntityWithNullId() {
        ProductCategory domain = new ProductCategory(null, "Toys", null);

        ProductCategoryEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getLabel()).isEqualTo("Toys");
        assertThat(entity.getDescription()).isNull();
    }

    @Test
    void toEntity_mapsNullValue() {
        ProductCategoryEntity entity = mapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        UUID id = UUID.randomUUID();
        ProductCategoryEntity entity = ProductCategoryEntity.builder()
                .id(id)
                .label("Music")
                .description("CDs and vinyl")
                .build();

        ProductCategory domain = mapper.toDomain(entity);

        assertThat(domain.id()).isEqualTo(id);
        assertThat(domain.label()).isEqualTo("Music");
        assertThat(domain.description()).isEqualTo("CDs and vinyl");
    }

    @Test
    void toDomain_mapsNullValue() {
        ProductCategory domain = mapper.toDomain(null);

        assertThat(domain).isNull();
    }
}
