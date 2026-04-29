package ara.project.takalo.budget.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.application.port.in.BudgetTransferResult;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetCreditRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetEditorAddRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetEditorResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetLightResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetMovementResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetResponse;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetTransferRequest;
import ara.project.takalo.budget.infrastructure.rest.dto.BudgetTransferResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.util.List;
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Supprimer un budget",
            description = "Supprime le budget. Les achats associés sont détachés (budgetId → null) sans être supprimés.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Budget supprimé"),
            @ApiResponse(responseCode = "403", description = "Utilisateur non éditeur du budget",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@Parameter(description = "Identifiant du budget") @PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Lister les budgets", description = "Listing paginé des budgets avec calcul du reste.")
    public PagedResponse<BudgetLightResponse> list(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {
        return service.findAll(page, size).map(mapper::toLightResponse);
    }

    @PostMapping("/{id}/credits")
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Créditer un budget depuis une source externe",
            description = "Augmente le fond initial du budget. Source à ce jour : INCONNUE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Crédit appliqué"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou montant non strictement positif",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Utilisateur non éditeur du budget",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetResponse credit(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id,
            @Valid @RequestBody BudgetCreditRequest request) {
        BudgetWithBalance updated = service.creditFromExternalSource(id, mapper.toCommand(request));
        return mapper.toResponse(updated);
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('PERM_budget:write')")
    @Operation(summary = "Transférer un montant entre deux budgets",
            description = "Déplace un montant strictement positif d'un budget source vers un budget cible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfert appliqué"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou règle métier violée",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Utilisateur non éditeur de l'un des budgets",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget source ou cible introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetTransferResponse transfer(@Valid @RequestBody BudgetTransferRequest request) {
        BudgetTransferResult result = service.transfer(mapper.toCommand(request));
        return new BudgetTransferResponse(
                mapper.toResponse(result.source()),
                mapper.toResponse(result.target())
        );
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Consulter l'historique des mouvements d'un budget",
            description = "Renvoie les mouvements du budget, ordonnés par date d'occurrence. Filtrable par type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mouvements trouvés"),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<BudgetMovementResponse> movements(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id,
            @Parameter(description = "Filtrer par type de mouvement (optionnel)")
            @RequestParam(required = false) BudgetMovementType type) {
        return service.findMovements(id, type).stream()
                .map(mapper::toMovementResponse)
                .toList();
    }

    @PostMapping("/{id}/editors")
    @PreAuthorize("hasAuthority('PERM_budget:manage-editors')")
    @Operation(summary = "Ajouter un éditeur à un budget",
            description = "Ajoute un utilisateur à la liste des éditeurs. Seul le créateur peut effectuer cette opération.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Éditeur ajouté"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Seul le créateur peut gérer la liste des éditeurs",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget ou utilisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Utilisateur déjà éditeur de ce budget",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetResponse addEditor(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id,
            @Valid @RequestBody BudgetEditorAddRequest request) {
        return mapper.toResponse(service.addEditor(id, request.userId()));
    }

    @DeleteMapping("/{id}/editors/{userId}")
    @PreAuthorize("hasAuthority('PERM_budget:manage-editors')")
    @Operation(summary = "Retirer un éditeur d'un budget",
            description = "Retire un utilisateur de la liste des éditeurs. Seul le créateur peut effectuer cette opération.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Éditeur retiré"),
            @ApiResponse(responseCode = "400", description = "Le créateur ne peut pas être retiré",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Seul le créateur peut gérer la liste des éditeurs",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget introuvable ou utilisateur non éditeur",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BudgetResponse removeEditor(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id,
            @Parameter(description = "Identifiant de l'utilisateur") @PathVariable UUID userId) {
        return mapper.toResponse(service.removeEditor(id, userId));
    }

    @GetMapping("/{id}/editors")
    @PreAuthorize("hasAuthority('PERM_budget:read')")
    @Operation(summary = "Lister les éditeurs d'un budget",
            description = "Renvoie la liste des éditeurs avec leur nom d'affichage et un drapeau indiquant le créateur. "
                    + "Lecture ouverte à tout utilisateur authentifié porteur de budget:read.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des éditeurs"),
            @ApiResponse(responseCode = "404", description = "Budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<BudgetEditorResponse> listEditors(
            @Parameter(description = "Identifiant du budget") @PathVariable UUID id) {
        return service.listEditors(id).stream()
                .map(mapper::toEditorResponse)
                .toList();
    }
}
