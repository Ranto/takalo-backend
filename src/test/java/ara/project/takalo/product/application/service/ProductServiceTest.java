package ara.project.takalo.product.application.service;

import ara.project.takalo.product.application.port.out.ProductRepository;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void create_whenNameNotTaken_savesAndReturns() {
        Product toSave = new Product(null, "Phone", UUID.randomUUID(), null, null);
        Product saved = new Product(UUID.randomUUID(), "Phone", toSave.categoryId(), Instant.now(), Instant.now());
        when(repository.existsByName("Phone")).thenReturn(false);
        when(repository.save(toSave)).thenReturn(saved);

        Product result = service.create(toSave);

        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_whenNameTaken_throwsAlreadyExists() {
        Product toSave = new Product(null, "Phone", UUID.randomUUID(), null, null);
        when(repository.existsByName("Phone")).thenReturn(true);

        assertThatThrownBy(() -> service.create(toSave))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void update_whenFoundAndNameUnchanged_savesNewDomainWithPathId() {
        UUID pathId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product existing = new Product(pathId, "Phone", UUID.randomUUID(), Instant.now(), Instant.now());
        Product body = new Product(UUID.randomUUID(), "Phone", categoryId, null, null);
        Product persisted = new Product(pathId, "Phone", categoryId, existing.createdAt(), Instant.now());

        when(repository.findById(pathId)).thenReturn(Optional.of(existing));
        when(repository.existsByName("Phone")).thenReturn(true);
        when(repository.save(any(Product.class))).thenReturn(persisted);

        Product result = service.update(pathId, body);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(pathId);
        assertThat(saved.name()).isEqualTo("Phone");
        assertThat(saved.categoryId()).isEqualTo(categoryId);
        assertThat(saved.createdAt()).isNull();
        assertThat(saved.updatedAt()).isNull();
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void update_whenFoundAndNameChangedAndFree_savesNewDomain() {
        UUID pathId = UUID.randomUUID();
        Product existing = new Product(pathId, "Old", UUID.randomUUID(), Instant.now(), Instant.now());
        Product body = new Product(null, "New", UUID.randomUUID(), null, null);
        Product persisted = new Product(pathId, "New", body.categoryId(), existing.createdAt(), Instant.now());

        when(repository.findById(pathId)).thenReturn(Optional.of(existing));
        when(repository.existsByName("New")).thenReturn(false);
        when(repository.save(any(Product.class))).thenReturn(persisted);

        Product result = service.update(pathId, body);

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void update_whenNameTakenByAnotherProduct_throwsAlreadyExists() {
        UUID pathId = UUID.randomUUID();
        Product existing = new Product(pathId, "Old", UUID.randomUUID(), Instant.now(), Instant.now());
        Product body = new Product(null, "Taken", UUID.randomUUID(), null, null);

        when(repository.findById(pathId)).thenReturn(Optional.of(existing));
        when(repository.existsByName("Taken")).thenReturn(true);

        assertThatThrownBy(() -> service.update(pathId, body))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void update_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        Product body = new Product(null, "x", UUID.randomUUID(), null, null);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, body))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(repository, never()).save(any());
    }

    @Test
    void delete_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void getById_whenFound_returnsDomain() {
        UUID id = UUID.randomUUID();
        Product found = new Product(id, "Phone", UUID.randomUUID(), Instant.now(), Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(found));

        Product result = service.getById(id);

        assertThat(result).isSameAs(found);
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchByCategoriesOrName_delegatesToRepository() {
        List<UUID> categoryIds = List.of(UUID.randomUUID());
        PagedResponse<Product> page = new PagedResponse<>(List.of(), 2, 7, 0L, 0, true);
        when(repository.findByNameOrCategoryIds("foo", categoryIds, 2, 7)).thenReturn(page);

        PagedResponse<Product> result = service.searchByCategoriesOrName(categoryIds, "foo", 2, 7);

        assertThat(result).isSameAs(page);
    }

    @Test
    void findAll_delegatesToRepository() {
        PagedResponse<Product> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(repository.findAll(0, 10)).thenReturn(page);

        PagedResponse<Product> result = service.findAll(0, 10);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getProductNames_delegatesToRepository() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        Map<UUID, String> names = Map.of(ids.iterator().next(), "Phone");
        when(repository.getProductNames(ids)).thenReturn(names);

        Map<UUID, String> result = service.getProductNames(ids);

        assertThat(result).isSameAs(names);
    }

    @Test
    void countByCategoryIds_whenIdsNull_returnsEmptyMap() {
        Map<UUID, Long> result = service.countByCategoryIds(null);

        assertThat(result).isEmpty();
        verify(repository, never()).countByCategoryIds(any());
    }

    @Test
    void countByCategoryIds_whenIdsEmpty_returnsEmptyMap() {
        Map<UUID, Long> result = service.countByCategoryIds(Set.of());

        assertThat(result).isEmpty();
        verify(repository, never()).countByCategoryIds(any());
    }

    @Test
    void countByCategoryIds_delegatesToRepository() {
        UUID categoryId = UUID.randomUUID();
        Set<UUID> ids = Set.of(categoryId);
        Map<UUID, Long> counts = Map.of(categoryId, 5L);
        when(repository.countByCategoryIds(ids)).thenReturn(counts);

        Map<UUID, Long> result = service.countByCategoryIds(ids);

        assertThat(result).isSameAs(counts);
    }
}
