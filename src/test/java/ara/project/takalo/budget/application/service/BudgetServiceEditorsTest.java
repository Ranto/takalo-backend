package ara.project.takalo.budget.application.service;

import ara.project.takalo.budget.application.port.out.BudgetMovementRepository;
import ara.project.takalo.budget.application.port.out.BudgetRepository;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetEditor;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.domain.model.User;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceEditorsTest {

    @Mock
    private BudgetRepository repository;

    @Mock
    private BudgetMovementRepository movementRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserServicePort userService;

    @Mock
    private UserDefaultBudgetServicePort defaultBudgetService;

    @Mock
    private PurchaseServicePort purchaseService;

    @InjectMocks
    private BudgetService service;

    private Budget budget(UUID id, UUID creator, UUID... otherEditors) {
        Set<UUID> editors = new java.util.HashSet<>();
        editors.add(creator);
        for (UUID e : otherEditors) editors.add(e);
        return new Budget(id, "Courses", null, new BigDecimal("500.00"),
                creator, editors, Instant.now(), null);
    }

    private User user(UUID id, String name) {
        return new User(id, "ext-" + id, name + "@example.com", name, Set.of(), Instant.now(), null);
    }

    private void stubBalance(UUID id) {
        when(repository.totalPurchasesByBudgetIds(List.of(id))).thenReturn(Map.of());
    }

    // ------------------------------------------------------------------
    // Ajout d'éditeur (S43, S49, S52, S54)
    // ------------------------------------------------------------------

    @Test
    void addEditor_byCreator_addsUserToEditors() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice)));
        when(currentUserProvider.id()).thenReturn(alice);
        when(userService.getById(bob)).thenReturn(user(bob, "bob"));
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        stubBalance(id);

        BudgetWithBalance result = service.addEditor(id, bob);

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().editorIds()).containsExactlyInAnyOrder(alice, bob);
        assertThat(result.budget().editorIds()).containsExactlyInAnyOrder(alice, bob);
    }

    @Test
    void addEditor_byNonCreator_throws403() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID carol = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice, bob)));
        when(currentUserProvider.id()).thenReturn(bob);

        assertThatThrownBy(() -> service.addEditor(id, carol))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Seul le créateur peut gérer la liste des éditeurs");

        verify(repository, never()).save(any());
    }

    @Test
    void addEditor_userAlreadyEditor_throws409() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice, bob)));
        when(currentUserProvider.id()).thenReturn(alice);
        when(userService.getById(bob)).thenReturn(user(bob, "bob"));

        assertThatThrownBy(() -> service.addEditor(id, bob))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessage("L'utilisateur est déjà éditeur de ce budget");

        verify(repository, never()).save(any());
    }

    @Test
    void addEditor_unknownUser_throws404() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID ghost = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice)));
        when(currentUserProvider.id()).thenReturn(alice);
        when(userService.getById(ghost))
                .thenThrow(new ResourceNotFoundException("Utilisateur non trouvé"));

        assertThatThrownBy(() -> service.addEditor(id, ghost))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void addEditor_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addEditor(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget non trouvé");
    }

    // ------------------------------------------------------------------
    // Retrait d'éditeur (S46, S50, S51, S53, S71/S73)
    // ------------------------------------------------------------------

    @Test
    void removeEditor_byCreator_removesUser_andClearsTheirDefaultBudget() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice, bob)));
        when(currentUserProvider.id()).thenReturn(alice);
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));
        stubBalance(id);

        BudgetWithBalance result = service.removeEditor(id, bob);

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().editorIds()).containsExactly(alice);
        assertThat(result.budget().editorIds()).containsExactly(alice);
        // S71/S73 : la préférence "budget par défaut" de l'utilisateur retiré est nettoyée.
        verify(defaultBudgetService).clearForUserIfBudgetMatches(eq(bob), eq(id));
    }

    @Test
    void removeEditor_byNonCreator_throws403() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice, bob)));
        when(currentUserProvider.id()).thenReturn(bob);

        assertThatThrownBy(() -> service.removeEditor(id, alice))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Seul le créateur peut gérer la liste des éditeurs");

        verify(repository, never()).save(any());
        verify(defaultBudgetService, never()).clearForUserIfBudgetMatches(any(), any());
    }

    @Test
    void removeEditor_creatorRemovingSelf_throws400() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.removeEditor(id, alice))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Le créateur ne peut pas être retiré de la liste des éditeurs");

        verify(repository, never()).save(any());
        verify(defaultBudgetService, never()).clearForUserIfBudgetMatches(any(), any());
    }

    @Test
    void removeEditor_userNotInList_throws404() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID carol = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice)));
        when(currentUserProvider.id()).thenReturn(alice);

        assertThatThrownBy(() -> service.removeEditor(id, carol))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("éditeur");

        verify(repository, never()).save(any());
        verify(defaultBudgetService, never()).clearForUserIfBudgetMatches(any(), any());
    }

    @Test
    void removeEditor_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeEditor(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget non trouvé");
    }

    // ------------------------------------------------------------------
    // Liste des éditeurs (S55)
    // ------------------------------------------------------------------

    @Test
    void listEditors_returnsAllEditors_withCreatorFlag() {
        UUID id = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(budget(id, alice, bob)));
        when(userService.getById(alice)).thenReturn(user(alice, "alice"));
        when(userService.getById(bob)).thenReturn(user(bob, "bob"));

        List<BudgetEditor> editors = service.listEditors(id);

        assertThat(editors).hasSize(2);
        assertThat(editors).filteredOn(BudgetEditor::creator)
                .extracting(BudgetEditor::userId).containsExactly(alice);
        assertThat(editors).extracting(BudgetEditor::userId)
                .containsExactlyInAnyOrder(alice, bob);
        assertThat(editors).extracting(BudgetEditor::displayName)
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void listEditors_unknownBudget_throws404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listEditors(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
