package ara.project.takalo.user.application.service;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.out.UserDefaultBudgetRepository;
import ara.project.takalo.user.domain.model.UserDefaultBudget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
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
class UserDefaultBudgetServiceTest {

    @Mock
    private UserDefaultBudgetRepository repository;

    @Mock
    private BudgetServicePort budgetService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserDefaultBudgetService service;

    private Budget budget(UUID id, UUID... editors) {
        return new Budget(id, "B", null, BigDecimal.ZERO, editors[0],
                Set.of(editors), Instant.now(), null);
    }

    @Test
    void getCurrent_returnsEmptyByDefault() {
        UUID alice = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.findByUserId(alice)).thenReturn(Optional.empty());

        UserDefaultBudget result = service.getCurrent();

        assertThat(result.isSet()).isFalse();
        assertThat(result.userId()).isEqualTo(alice);
    }

    @Test
    void setCurrent_byEditor_persists() {
        UUID alice = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(alice);
        when(budgetService.getRawById(budgetId)).thenReturn(budget(budgetId, alice));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDefaultBudget result = service.setCurrent(budgetId);

        ArgumentCaptor<UserDefaultBudget> captor = ArgumentCaptor.forClass(UserDefaultBudget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(alice);
        assertThat(captor.getValue().budgetId()).isEqualTo(budgetId);
        assertThat(result.budgetId()).isEqualTo(budgetId);
    }

    @Test
    void setCurrent_byNonEditor_throws403_withFrenchMessage() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(alice);
        when(budgetService.getRawById(budgetId)).thenReturn(budget(budgetId, bob));

        assertThatThrownBy(() -> service.setCurrent(budgetId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Le budget par défaut doit être un budget dont vous êtes éditeur");

        verify(repository, never()).save(any());
    }

    @Test
    void setCurrent_unknownBudget_throws404() {
        UUID budgetId = UUID.randomUUID();
        when(budgetService.getRawById(budgetId))
                .thenThrow(new ResourceNotFoundException("Budget non trouvé"));

        assertThatThrownBy(() -> service.setCurrent(budgetId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void clearCurrent_setsEmpty() {
        UUID alice = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDefaultBudget result = service.clearCurrent();

        ArgumentCaptor<UserDefaultBudget> captor = ArgumentCaptor.forClass(UserDefaultBudget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().budgetId()).isNull();
        assertThat(result.isSet()).isFalse();
    }

    @Test
    void clearForUserIfBudgetMatches_matches_clears() {
        UUID alice = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(repository.findByUserId(alice))
                .thenReturn(Optional.of(new UserDefaultBudget(alice, budgetId, Instant.now())));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.clearForUserIfBudgetMatches(alice, budgetId);

        ArgumentCaptor<UserDefaultBudget> captor = ArgumentCaptor.forClass(UserDefaultBudget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().budgetId()).isNull();
    }

    @Test
    void clearForUserIfBudgetMatches_otherBudget_doesNothing() {
        UUID alice = UUID.randomUUID();
        UUID currentDefault = UUID.randomUUID();
        UUID otherBudget = UUID.randomUUID();
        when(repository.findByUserId(alice))
                .thenReturn(Optional.of(new UserDefaultBudget(alice, currentDefault, Instant.now())));

        service.clearForUserIfBudgetMatches(alice, otherBudget);

        verify(repository, never()).save(any());
    }

    @Test
    void clearForUserIfBudgetMatches_noPreference_doesNothing() {
        UUID alice = UUID.randomUUID();
        when(repository.findByUserId(alice)).thenReturn(Optional.empty());

        service.clearForUserIfBudgetMatches(alice, UUID.randomUUID());

        verify(repository, never()).save(any());
    }

    @Test
    void resolveDefaultBudgetIdFor_returnsValue() {
        UUID alice = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        when(repository.findByUserId(alice))
                .thenReturn(Optional.of(new UserDefaultBudget(alice, budgetId, Instant.now())));

        assertThat(service.resolveDefaultBudgetIdFor(alice)).contains(budgetId);
    }

    @Test
    void resolveDefaultBudgetIdFor_noPreference_returnsEmpty() {
        UUID alice = UUID.randomUUID();
        when(repository.findByUserId(alice)).thenReturn(Optional.empty());

        assertThat(service.resolveDefaultBudgetIdFor(alice)).isEmpty();
    }
}
