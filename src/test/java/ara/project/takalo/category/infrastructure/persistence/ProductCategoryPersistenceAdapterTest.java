package ara.project.takalo.category.infrastructure.persistence;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.persistence.entities.ProductCategoryEntity;
import ara.project.takalo.category.infrastructure.persistence.mappers.ProductCategoryMapper;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryPersistenceAdapterTest {

    @Mock
    private JpaProductCategoryRepository jpaRepository;

    @Mock
    private ProductCategoryMapper mapper;

    @InjectMocks
    private ProductCategoryPersistenceAdapter adapter;

    @Test
    void save_mapsToEntityPersistsAndMapsBack() {
        ProductCategory domain = new ProductCategory(null, "Books", "desc");
        ProductCategoryEntity toPersist = ProductCategoryEntity.builder().label("Books").description("desc").build();
        ProductCategoryEntity persisted = ProductCategoryEntity.builder()
                .id(UUID.randomUUID()).label("Books").description("desc").build();
        ProductCategory mappedBack = new ProductCategory(persisted.getId(), "Books", "desc");

        when(mapper.toEntity(domain)).thenReturn(toPersist);
        when(jpaRepository.save(toPersist)).thenReturn(persisted);
        when(mapper.toDomain(persisted)).thenReturn(mappedBack);

        ProductCategory result = adapter.save(domain);

        assertThat(result).isSameAs(mappedBack);
    }

    @Test
    void findById_whenPresent_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        ProductCategoryEntity entity = ProductCategoryEntity.builder().id(id).label("L").build();
        ProductCategory domain = new ProductCategory(id, "L", null);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ProductCategory> result = adapter.findById(id);

        assertThat(result).contains(domain);
    }

    @Test
    void findById_whenAbsent_returnsEmptyOptional() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ProductCategory> result = adapter.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void findByLabel_buildsPageRequestAndConvertsViaPaginationMapper() {
        UUID id = UUID.randomUUID();
        ProductCategoryEntity entity = ProductCategoryEntity.builder().id(id).label("Books").build();
        ProductCategory domain = new ProductCategory(id, "Books", null);
        PageRequest expected = PageRequest.of(0, 5);
        PageImpl<ProductCategoryEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findByLabelContainingIgnoreCase(eq("Books"), pageableCaptor.capture())).thenReturn(page);
        when(mapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<ProductCategory> result = adapter.findByLabel("Books", 0, 5);

        assertThat(pageableCaptor.getValue()).isEqualTo(expected);
        assertThat(result.content()).containsExactly(domain);
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void findAll_buildsPageRequestAndConvertsViaPaginationMapper() {
        UUID id = UUID.randomUUID();
        ProductCategoryEntity entity = ProductCategoryEntity.builder().id(id).label("X").build();
        ProductCategory domain = new ProductCategory(id, "X", null);
        PageRequest expected = PageRequest.of(0, 10);
        PageImpl<ProductCategoryEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findAll(pageableCaptor.capture())).thenReturn(page);
        when(mapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<ProductCategory> result = adapter.findAll(0, 10);

        assertThat(pageableCaptor.getValue()).isEqualTo(expected);
        assertThat(result.content()).containsExactly(domain);
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
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
    void existsByLabel_whenLabelExists_returnsTrue() {
        when(jpaRepository.existsByLabelIgnoreCase("Books")).thenReturn(true);

        boolean result = adapter.existsByLabel("Books");

        assertThat(result).isTrue();
        verify(jpaRepository).existsByLabelIgnoreCase("Books");
    }

    @Test
    void existsByLabel_whenLabelDoesNotExist_returnsFalse() {
        when(jpaRepository.existsByLabelIgnoreCase("Unknown")).thenReturn(false);

        boolean result = adapter.existsByLabel("Unknown");

        assertThat(result).isFalse();
        verify(jpaRepository).existsByLabelIgnoreCase("Unknown");
    }
}
