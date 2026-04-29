package ara.project.takalo.budget.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetLightResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetUpdateRequest;
import ara.project.takalo.budget.infrastructure.rest.mapper.BudgetWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.rest.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Budgets", description = "Gestion des budgets")
public class BudgetController {

    private final BudgetServicePort service;
    private final BudgetWebMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_budget:create')")
    @Operation(summary = "Créer un budget", description = "Crée un nouveau budget avec un fond initial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Budget créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Un budget avec ce nom existe déjà",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        Budget toCreate = mapper.toDomain(request);
        Budget created = service.create(toCreate);
        return mapper.toResponse(new BudgetWithBalance(created, BigDecimal.ZERO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Obtenir un budget par identifiant",
            description = "Renvoie le budget avec son reste (fond − somme des achats associés).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget trouvé"),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BudgetResponse> getById(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id) {
        BudgetWithBalance bwb = service.getById(id);
        return ResponseEntity.ok(mapper.toResponse(bwb));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Mettre à jour un budget",
            description = "Met à jour le nom et/ou la description d'un budget. Le fond initial ne peut pas être modifié.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget mis à jour"),
            @ApiResponse(responseCode = "400", description = "Opération invalide ou données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Utilisateur non éditeur du budget",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Un budget avec ce nom existe déjà",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetResponse update(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id,
            @Valid @RequestBody BudgetUpdateRequest request) {
        BudgetWithBalance updated = service.update(id, mapper.toCommand(request));
        return mapper.toResponse(updated);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Lister les budgets", description = "Listing paginé des budgets avec calcul du reste.")
    public PagedResponse<BudgetLightResponse> list(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {
        return service.findAll(page, size).map(mapper::toLightResponse);
    }
}
