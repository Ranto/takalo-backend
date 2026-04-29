package ara.project.takalo.user.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.domain.model.UserDefaultBudget;
import ara.project.takalo.user.infrastructure.rest.dto.DefaultBudgetRequest;
import ara.project.takalo.user.infrastructure.rest.dto.DefaultBudgetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/default-budget")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Budget par défaut", description = "Préférence du budget par défaut de l'utilisateur courant")
public class UserDefaultBudgetController {

    private final UserDefaultBudgetServicePort service;
    private final BudgetServicePort budgetService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Consulter le budget par défaut de l'utilisateur courant")
    public DefaultBudgetResponse get() {
        return toResponse(service.getCurrent());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Définir le budget par défaut de l'utilisateur courant")
    public DefaultBudgetResponse set(@Valid @RequestBody DefaultBudgetRequest request) {
        return toResponse(service.setCurrent(request.budgetId()));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Retirer le budget par défaut de l'utilisateur courant")
    public DefaultBudgetResponse clear() {
        return toResponse(service.clearCurrent());
    }

    private DefaultBudgetResponse toResponse(UserDefaultBudget pref) {
        if (!pref.isSet()) {
            return new DefaultBudgetResponse(null, null);
        }
        Budget budget = budgetService.getRawById(pref.budgetId());
        return new DefaultBudgetResponse(budget.id(), budget.name());
    }
}
