package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class BudgetServiceTest {

    @Mock
    private BudgetRepository repository;

    @Mock
    private BudgetMovementRepository movementRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private BudgetService service;

    private Budget input(String name, BigDecimal fond) {
        return new Budget(null, name, "desc", fond, null, null, null, null);
    }

    @Test
    void create_setsCurrentUserAsCreatorAndSoleEditor() {
        UUID currentUser = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.existsByName("Courses")).thenReturn(false);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        Budget result = service.create(input("Courses", new BigDecimal("500.00")));

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(captor.capture());
        Budget passed = captor.getValue();
        assertThat(passed.createdBy()).isEqualTo(currentUser);
        assertThat(passed.editorIds()).containsExactly(currentUser);
        assertThat(passed.initialFund()).isEqualByComparingTo("500.00");
        assertThat(result.name()).isEqualTo("Courses");
    }

    @Test
    void create_acceptsNegativeAndZeroFunds() {
        UUID currentUser = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(currentUser);
        when(repository.existsByName(any())).thenReturn(false);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input("Negatif", new BigDecimal("-100.00")));
        service.create(input("Zero", BigDecimal.ZERO));

        verify(repository, org.mockito.Mockito.times(2)).save(any(Budget.class));
    }

    @Test
    void create_whenNameAlreadyExists_throwsAlreadyExists() {
        when(repository.existsByName("Dup")).thenReturn(true);

        assertThatThrownBy(() -> service.create(input("Dup", new BigDecimal("10.00"))))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void getById_whenNoPurchases_resteEqualsFond() {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("500.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        when(repository.findById(id)).thenReturn(Optional.of(budget));
        when(repository.totalPurchasesByBudgetIds(List.of(id))).thenReturn(Map.of());

        BudgetWithBalance result = service.getById(id);

        assertThat(result.budget()).isSameAs(budget);
        assertThat(result.totalPurchases()).isEqualByComparingTo("0");
    }

    @Test
    void getById_whenPurchasesExceedFund_resteIsNegative() {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        when(repository.findById(id)).thenReturn(Optional.of(budget));
        when(repository.totalPurchasesByBudgetIds(List.of(id)))
                .thenReturn(Map.of(id, new BigDecimal("150.00")));

        BudgetWithBalance result = service.getById(id);

        assertThat(result.totalPurchases()).isEqualByComparingTo("150.00");
        assertThat(budget.initialFund().subtract(result.totalPurchases()))
                .isEqualByComparingTo("-50.00");
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget non trouvé");
    }

    @Test
    void findAll_computesBalanceForEachBudgetInOneCall() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Budget b1 = new Budget(id1, "A", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        Budget b2 = new Budget(id2, "B", null, new BigDecimal("200.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        PagedResponse<Budget> page = new PagedResponse<>(List.of(b1, b2), 0, 10, 2L, 1, true);
        when(repository.findAll(0, 10)).thenReturn(page);
        when(repository.totalPurchasesByBudgetIds(List.of(id1, id2)))
                .thenReturn(Map.of(id1, new BigDecimal("30.00")));

        PagedResponse<BudgetWithBalance> result = service.findAll(0, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).totalPurchases()).isEqualByComparingTo("30.00");
        assertThat(result.content().get(1).totalPurchases()).isEqualByComparingTo("0");
        verify(repository).totalPurchasesByBudgetIds(List.of(id1, id2));
    }

    @Test
    void findAll_emptyPage_skipsTotalsCall() {
        PagedResponse<Budget> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(repository.findAll(0, 10)).thenReturn(page);

        PagedResponse<BudgetWithBalance> result = service.findAll(0, 10);

        assertThat(result.content()).isEmpty();
        verify(repository, never()).totalPurchasesByBudgetIds(any());
    }
}
