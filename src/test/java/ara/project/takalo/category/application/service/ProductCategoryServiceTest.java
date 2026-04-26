package ara.project.takalo.category.application.service;

import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.application.port.out.ProductCategoryRepository;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    @InjectMocks
    private ProductCategoryService service;

    @Test
    void create_delegatesToRepositoryAndReturnsSaved() {
        ProductCategory toSave = new ProductCategory(null, "Books", "desc");
        ProductCategory saved = new ProductCategory(UUID.randomUUID(), "Books", "desc");
        when(repository.save(toSave)).thenReturn(saved);

        ProductCategory result = service.create(toSave);

        assertThat(result).isSameAs(saved);
    }

    @Test
    void update_whenFound_savesNewDomainWithPathId() {
        UUID pathId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        ProductCategory existing = new ProductCategory(pathId, "Old", "old desc");
        ProductCategory body = new ProductCategory(otherId, "New", "new desc");
        ProductCategory persisted = new ProductCategory(pathId, "New", "new desc");

        when(repository.findById(pathId)).thenReturn(Optional.of(existing));
        when(repository.save(any(ProductCategory.class))).thenReturn(persisted);

        ProductCategory result = service.update(pathId, body);

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(repository).save(captor.capture());
        ProductCategory saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(pathId);
        assertThat(saved.label()).isEqualTo("New");
        assertThat(saved.description()).isEqualTo("new desc");
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        ProductCategory body = new ProductCategory(null, "x", "y");
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
        ProductCategory found = new ProductCategory(id, "Books", "desc");
        when(repository.findById(id)).thenReturn(Optional.of(found));

        ProductCategory result = service.getById(id);

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
    void search_whenLabelNull_callsFindAll() {
        PagedResponse<ProductCategory> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(repository.findAll(0, 10)).thenReturn(page);

        PagedResponse<ProductCategory> result = service.search(null, 0, 10);

        assertThat(result).isSameAs(page);
        verify(repository, never()).findByLabel(any(), anyInt(), anyInt());
    }

    @Test
    void search_whenLabelBlank_callsFindAll() {
        PagedResponse<ProductCategory> page = new PagedResponse<>(List.of(), 1, 5, 0L, 0, true);
        when(repository.findAll(1, 5)).thenReturn(page);

        PagedResponse<ProductCategory> result = service.search("   ", 1, 5);

        assertThat(result).isSameAs(page);
        verify(repository, never()).findByLabel(any(), anyInt(), anyInt());
    }

    @Test
    void search_whenLabelPresent_callsFindByLabel() {
        PagedResponse<ProductCategory> page = new PagedResponse<>(List.of(), 2, 7, 0L, 0, true);
        when(repository.findByLabel("foo", 2, 7)).thenReturn(page);

        PagedResponse<ProductCategory> result = service.search("foo", 2, 7);

        assertThat(result).isSameAs(page);
        verify(repository, never()).findAll(anyInt(), anyInt());
    }
}
