package ara.project.takalo.product.infrastructure.persistence;

import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.persistence.entities.ProductEntity;
import ara.project.takalo.product.infrastructure.persistence.mappers.ProductMapper;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPersistenceAdapterTest {

    @Mock
    private JpaProductRepository jpaRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductPersistenceAdapter adapter;

    @Test
    void save_mapsToEntityPersistsAndMapsBack() {
        Product domain = new Product(null, "Phone", UUID.randomUUID(), null, null);
        ProductEntity toPersist = ProductEntity.builder().name("Phone").categoryId(domain.categoryId()).build();
        ProductEntity persisted = ProductEntity.builder()
                .id(UUID.randomUUID()).name("Phone").categoryId(domain.categoryId()).build();
        Product mappedBack = new Product(persisted.getId(), "Phone", domain.categoryId(), null, null);

        when(productMapper.toEntity(domain)).thenReturn(toPersist);
        when(jpaRepository.save(toPersist)).thenReturn(persisted);
        when(productMapper.toDomain(persisted)).thenReturn(mappedBack);

        Product result = adapter.save(domain);

        assertThat(result).isSameAs(mappedBack);
    }

    @Test
    void findById_whenPresent_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = ProductEntity.builder().id(id).name("Phone").build();
        Product domain = new Product(id, "Phone", null, null, null);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(productMapper.toDomain(entity)).thenReturn(domain);

        Optional<Product> result = adapter.findById(id);

        assertThat(result).contains(domain);
    }

    @Test
    void findById_whenAbsent_returnsEmptyOptional() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<Product> result = adapter.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_callsJpaWithNullFiltersAndConverts() {
        UUID id = UUID.randomUUID();
        ProductEntity entity = ProductEntity.builder().id(id).name("Phone").build();
        Product domain = new Product(id, "Phone", null, null, null);
        PageRequest expected = PageRequest.of(0, 10);
        PageImpl<ProductEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findAllWithNameOrCategoryIdIn(eq(null), eq(null), pageableCaptor.capture()))
                .thenReturn(page);
        when(productMapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<Product> result = adapter.findAll(0, 10);

        assertThat(pageableCaptor.getValue()).isEqualTo(expected);
        assertThat(result.content()).containsExactly(domain);
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void findByNameOrCategoryIds_passesFiltersAndConverts() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        List<UUID> categoryIds = List.of(categoryId);
        ProductEntity entity = ProductEntity.builder().id(id).name("Phone").categoryId(categoryId).build();
        Product domain = new Product(id, "Phone", categoryId, null, null);
        PageRequest expected = PageRequest.of(1, 5);
        PageImpl<ProductEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findAllWithNameOrCategoryIdIn(eq("Pho"), eq(categoryIds), pageableCaptor.capture()))
                .thenReturn(page);
        when(productMapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<Product> result = adapter.findByNameOrCategoryIds("Pho", categoryIds, 1, 5);

        assertThat(pageableCaptor.getValue()).isEqualTo(expected);
        assertThat(result.content()).containsExactly(domain);
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(5);
    }

    @Test
    void deleteById_whenExists_callsDelete() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.existsById(id)).thenReturn(true);

        adapter.deleteById(id);

        verify(jpaRepository).deleteById(id);
    }

    @Test
    void deleteById_whenAbsent_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> adapter.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(jpaRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void existsByName_delegatesIgnoreCase() {
        when(jpaRepository.existsByNameIgnoreCase("Phone")).thenReturn(true);

        boolean result = adapter.existsByName("Phone");

        assertThat(result).isTrue();
    }

    @Test
    void getProductNames_whenIdsNull_returnsEmptyMap() {
        Map<UUID, String> result = adapter.getProductNames(null);

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).findNamesById(any());
    }

    @Test
    void getProductNames_whenIdsEmpty_returnsEmptyMap() {
        Map<UUID, String> result = adapter.getProductNames(Set.of());

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).findNamesById(any());
    }

    @Test
    void getProductNames_buildsMapFromProjections() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        JpaProductRepository.ProductIdAndName p1 = projection(id1, "Phone");
        JpaProductRepository.ProductIdAndName p2 = projection(id2, "Tablet");
        when(jpaRepository.findNamesById(ids)).thenReturn(List.of(p1, p2));

        Map<UUID, String> result = adapter.getProductNames(ids);

        assertThat(result).containsEntry(id1, "Phone").containsEntry(id2, "Tablet");
    }

    private static JpaProductRepository.ProductIdAndName projection(UUID id, String name) {
        return new JpaProductRepository.ProductIdAndName() {
            @Override public UUID getId() { return id; }
            @Override public String getName() { return name; }
        };
    }

    @Test
    void countByCategoryIds_whenIdsNull_returnsEmptyMap() {
        Map<UUID, Long> result = adapter.countByCategoryIds(null);

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).countByCategoryIds(any());
    }

    @Test
    void countByCategoryIds_whenIdsEmpty_returnsEmptyMap() {
        Map<UUID, Long> result = adapter.countByCategoryIds(Set.of());

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).countByCategoryIds(any());
    }

    @Test
    void countByCategoryIds_buildsMapFromProjections() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(c1, c2);

        JpaProductRepository.CategoryProductCount p1 = countProjection(c1, 3L);
        JpaProductRepository.CategoryProductCount p2 = countProjection(c2, 7L);
        when(jpaRepository.countByCategoryIds(ids)).thenReturn(List.of(p1, p2));

        Map<UUID, Long> result = adapter.countByCategoryIds(ids);

        assertThat(result).containsEntry(c1, 3L).containsEntry(c2, 7L);
    }

    private static JpaProductRepository.CategoryProductCount countProjection(UUID categoryId, long count) {
        return new JpaProductRepository.CategoryProductCount() {
            @Override public UUID getCategoryId() { return categoryId; }
            @Override public long getCount() { return count; }
        };
    }
}
