package ara.project.takalo.purchase.infrastructure.persistence;

import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.infrastructure.persistence.entities.PurchaseEntity;
import ara.project.takalo.purchase.infrastructure.persistence.mappers.PurchaseMapper;
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

import java.time.Instant;
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
class PurchasePersistenceAdapterTest {

    @Mock
    private JpaPurchaseRepository jpaRepository;

    @Mock
    private PurchaseMapper purchaseMapper;

    @InjectMocks
    private PurchasePersistenceAdapter adapter;

    @Test
    void save_mapsToEntityPersistsAndMapsBack() {
        Purchase domain = new Purchase(null, Instant.now(), List.of());
        PurchaseEntity toPersist = PurchaseEntity.builder().build();
        PurchaseEntity persisted = PurchaseEntity.builder().id(UUID.randomUUID()).build();
        Purchase mappedBack = new Purchase(persisted.getId(), domain.purchaseDate(), List.of());

        when(purchaseMapper.toEntity(domain)).thenReturn(toPersist);
        when(jpaRepository.save(toPersist)).thenReturn(persisted);
        when(purchaseMapper.toDomain(persisted)).thenReturn(mappedBack);

        Purchase result = adapter.save(domain);

        assertThat(result).isSameAs(mappedBack);
    }

    @Test
    void findById_whenPresent_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        PurchaseEntity entity = PurchaseEntity.builder().id(id).build();
        Purchase domain = new Purchase(id, Instant.now(), List.of());

        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(purchaseMapper.toDomain(entity)).thenReturn(domain);

        Optional<Purchase> result = adapter.findById(id);

        assertThat(result).contains(domain);
    }

    @Test
    void findById_whenAbsent_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    void findAll_passesPageableAndConverts() {
        UUID id = UUID.randomUUID();
        PurchaseEntity entity = PurchaseEntity.builder().id(id).build();
        Purchase domain = new Purchase(id, Instant.now(), List.of());
        PageRequest expected = PageRequest.of(0, 10);
        PageImpl<PurchaseEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findAll(captor.capture())).thenReturn(page);
        when(purchaseMapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<Purchase> result = adapter.findAll(0, 10);

        assertThat(captor.getValue()).isEqualTo(expected);
        assertThat(result.content()).containsExactly(domain);
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void findByDateRange_passesFiltersAndConverts() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        UUID id = UUID.randomUUID();
        PurchaseEntity entity = PurchaseEntity.builder().id(id).build();
        Purchase domain = new Purchase(id, Instant.now(), List.of());
        PageRequest expected = PageRequest.of(1, 5);
        PageImpl<PurchaseEntity> page = new PageImpl<>(List.of(entity), expected, 1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(jpaRepository.findByDateRange(eq(start), eq(end), captor.capture())).thenReturn(page);
        when(purchaseMapper.toDomain(entity)).thenReturn(domain);

        PagedResponse<Purchase> result = adapter.findByDateRange(start, end, 1, 5);

        assertThat(captor.getValue()).isEqualTo(expected);
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
    void deleteById_whenAbsent_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> adapter.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Achat non trouvé");

        verify(jpaRepository, never()).deleteById(any(UUID.class));
    }
}
