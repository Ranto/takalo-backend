package ara.project.takalo.purchase.infrastructure.rest;

import ara.project.takalo.purchase.application.port.in.BulkReassignBudgetResult;
import ara.project.takalo.purchase.application.port.in.CategoryBreakdownQuery;
import ara.project.takalo.purchase.application.port.in.PurchaseImportServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseItemDetailQuery;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.CategorySpendingBreakdown;
import ara.project.takalo.purchase.infrastructure.rest.dto.CategorySpendingBreakdownResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import org.openapitools.jackson.nullable.JsonNullable;
import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.domain.model.PurchaseImportValidationResult;
import ara.project.takalo.purchase.domain.model.PurchaseItemDetail;
import ara.project.takalo.purchase.infrastructure.rest.dto.BulkPurchaseIdsRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.BulkReassignPurchaseBudgetRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.BulkReassignPurchaseBudgetResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportValidationResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseItemDetailResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseLightResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.ReassignPurchaseBudgetRequest;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseImportWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseItemDetailWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseLightWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseWebMapper;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Achats", description = "Gestion des achats et de leurs articles")
public class PurchaseController {

    private final PurchaseServicePort service;
    private final PurchaseImportServicePort importService;
    private final PurchaseWebMapper purchaseWebMapper;
    private final PurchaseLightWebMapper purchaseLightWebMapper;
    private final PurchaseItemDetailWebMapper purchaseItemDetailWebMapper;
    private final PurchaseImportWebMapper purchaseImportWebMapper;
    private final UserDefaultBudgetServicePort defaultBudgetService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_purchase:create')")
    @Operation(summary = "Créer un achat", description = "Crée un nouvel achat avec ses articles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Achat créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produit référencé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PurchaseResponse create(@Valid @RequestBody PurchaseRequest request) {
        UUID effectiveBudgetId;
        JsonNullable<UUID> requested = request.budgetId();
        if (requested == null || !requested.isPresent()) {
            effectiveBudgetId = defaultBudgetService
                    .resolveDefaultBudgetIdFor(currentUserProvider.id())
                    .orElse(null);
        } else {
            effectiveBudgetId = requested.get();
        }
        Purchase purchase = purchaseWebMapper.toDomain(request, effectiveBudgetId);
        Purchase saved = service.create(purchase);
        return purchaseWebMapper.toResponse(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_purchase:read:any')")
    @Operation(summary = "Mettre à jour un achat")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achat mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PurchaseResponse> update(
            @Parameter(description = "Identifiant de l'achat") @PathVariable UUID id,
            @Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = purchaseWebMapper.toDomain(request);
        Purchase update = service.update(id, purchase);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(update));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Supprimer un achat",
            description = "Supprime l'achat et toutes ses lignes. " +
                    "Les utilisateurs avec :read:own ne peuvent supprimer que leurs propres achats.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Achat supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (achat appartenant à un autre utilisateur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@Parameter(description = "Identifiant de l'achat") @PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Obtenir un achat par identifiant", description = "Renvoie l'achat complet avec ses articles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achat trouvé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (achat appartenant à un autre utilisateur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PurchaseResponse> getById(
            @Parameter(description = "Identifiant de l'achat") @PathVariable UUID id) {
        Purchase purchase = service.getById(id);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(purchase));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Rechercher des achats",
            description = "Recherche paginée par fenêtre temporelle (start/end optionnels). " +
                    "Les utilisateurs avec :read:own ne voient que leurs propres achats.")
    public PagedResponse<PurchaseLightResponse> search(
            @Parameter(description = "Date de début (ISO-8601, inclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,

            @Parameter(description = "Date de fin (ISO-8601, exclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,

            @Parameter(description = "Filtre sur l'état de verrouillage : true = verrouillés, false = non verrouillés, omis = tous")
            @RequestParam(required = false) Boolean locked,

            @Parameter(description = "Filtre exact sur l'identifiant du budget. Combinable avec includeUnbudgeted pour inclure les achats sans budget.")
            @RequestParam(required = false) UUID budgetId,

            @Parameter(description = "Inclure aussi les achats sans budget. Si budgetId est omis, ne renvoie que les achats sans budget.")
            @RequestParam(defaultValue = "false") boolean includeUnbudgeted,

            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {
        PagedResponse<Purchase> result = service.search(start, end, locked, budgetId, includeUnbudgeted, page, size);
        return result.map(purchaseLightWebMapper::toResponse);
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Lister les lignes d'achat (vue détaillée)",
            description = "Recherche paginée des lignes d'achats confondus, avec date, produit, catégorie, " +
                    "prix unitaire, quantité, remise, total et magasin. Filtres : intervalle de date, nom du produit, " +
                    "nom de la catégorie. Tri : date (défaut), produit, catégorie. Les utilisateurs avec :read:own " +
                    "ne voient que leurs propres lignes.")
    public PagedResponse<PurchaseItemDetailResponse> searchItemDetails(
            @Parameter(description = "Date de début (ISO-8601, inclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,

            @Parameter(description = "Date de fin (ISO-8601, inclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,

            @Parameter(description = "Filtre partiel sur le nom du produit (insensible à la casse)")
            @RequestParam(required = false) String productName,

            @Parameter(description = "Filtre partiel sur le libellé de la catégorie (insensible à la casse)")
            @RequestParam(required = false) String categoryName,

            @Parameter(description = "Filtre exact sur l'identifiant du budget de l'achat parent")
            @RequestParam(required = false) UUID budgetId,

            @Parameter(description = "Inclure aussi les lignes d'achat sans budget. Si budgetId est omis, ne renvoie que les lignes sans budget.")
            @RequestParam(defaultValue = "false") boolean includeUnbudgeted,

            @Parameter(description = "Filtre exact sur l'identifiant du produit")
            @RequestParam(required = false) UUID productId,

            @Parameter(description = "Filtre exact sur l'identifiant de la catégorie du produit")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Champ de tri : date | product | category", example = "date")
            @RequestParam(defaultValue = "date") String sort,

            @Parameter(description = "Sens de tri : asc | desc", example = "desc")
            @RequestParam(required = false) String direction,

            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {

        PurchaseItemDetailQuery.SortField sortField = parseSort(sort);
        PurchaseItemDetailQuery.SortDirection dir = parseDirection(direction, sortField);
        PurchaseItemDetailQuery query = new PurchaseItemDetailQuery(
                start, end, productName, categoryName, budgetId, includeUnbudgeted, productId, categoryId,
                sortField, dir, page, size);
        PagedResponse<PurchaseItemDetail> result = service.searchItemDetails(query);
        return result.map(purchaseItemDetailWebMapper::toResponse);
    }

    private PurchaseItemDetailQuery.SortField parseSort(String sort) {
        if (sort == null) return PurchaseItemDetailQuery.SortField.DATE;
        return switch (sort.trim().toLowerCase()) {
            case "product" -> PurchaseItemDetailQuery.SortField.PRODUCT;
            case "category" -> PurchaseItemDetailQuery.SortField.CATEGORY;
            case "date", "" -> PurchaseItemDetailQuery.SortField.DATE;
            default -> throw new ara.project.takalo.shared.domain.exception.InvalidOperationException(
                    "Tri invalide. Valeurs acceptées : date, product, category");
        };
    }

    private PurchaseItemDetailQuery.SortDirection parseDirection(
            String direction, PurchaseItemDetailQuery.SortField sortField) {
        if (direction == null || direction.isBlank()) {
            return sortField == PurchaseItemDetailQuery.SortField.DATE
                    ? PurchaseItemDetailQuery.SortDirection.DESC
                    : PurchaseItemDetailQuery.SortDirection.ASC;
        }
        return switch (direction.trim().toLowerCase()) {
            case "asc" -> PurchaseItemDetailQuery.SortDirection.ASC;
            case "desc" -> PurchaseItemDetailQuery.SortDirection.DESC;
            default -> throw new ara.project.takalo.shared.domain.exception.InvalidOperationException(
                    "Sens de tri invalide. Valeurs acceptées : asc, desc");
        };
    }

    @GetMapping("/stats/categories")
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Répartition des dépenses par catégorie",
            description = "Agrège la somme des dépenses (unitPrice * quantité - remise) regroupées par " +
                    "catégorie de produit. Sans filtre de budget, agrège tous les achats accessibles. " +
                    "Plusieurs budgetId peuvent être fournis ; includeUnbudgeted=true ajoute les achats " +
                    "sans budget. Les utilisateurs avec :read:own ne voient que leurs propres achats.")
    public List<CategorySpendingBreakdownResponse> categoryBreakdown(
            @Parameter(description = "Date de début (ISO-8601, inclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,

            @Parameter(description = "Date de fin (ISO-8601, inclusif)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,

            @Parameter(description = "Identifiants de budget à inclure (répétable). Vide = tous les budgets.")
            @RequestParam(name = "budgetId", required = false) List<UUID> budgetIds,

            @Parameter(description = "Inclure aussi les achats sans budget.")
            @RequestParam(defaultValue = "false") boolean includeUnbudgeted) {
        CategoryBreakdownQuery query = new CategoryBreakdownQuery(start, end, budgetIds, includeUnbudgeted);
        return service.categoryBreakdown(query).stream()
                .map(this::toResponse)
                .toList();
    }

    private CategorySpendingBreakdownResponse toResponse(CategorySpendingBreakdown b) {
        return new CategorySpendingBreakdownResponse(
                b.categoryId(), b.categoryLabel(), b.total(), b.itemCount());
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Verrouiller un achat",
            description = "Verrouille l'achat contre toute modification (mise à jour, suppression, " +
                    "réassignation de budget). Une fois verrouillé, seul un SUPER_ADMIN peut " +
                    "modifier l'achat ou le déverrouiller. Le verrouillage peut être posé par " +
                    "l'auteur de l'achat ou par un SUPER_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achat verrouillé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (achat appartenant à un autre utilisateur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PurchaseResponse> lock(
            @Parameter(description = "Identifiant de l'achat") @PathVariable UUID id) {
        Purchase locked = service.lock(id);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(locked));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasAnyAuthority('PERM_purchase:read:own', 'PERM_purchase:read:any')")
    @Operation(summary = "Verrouiller plusieurs achats",
            description = "Opération en tout-ou-rien : si un achat est introuvable ou n'appartient pas " +
                    "à l'utilisateur courant (sauf SUPER_ADMIN), aucun verrouillage n'est appliqué. " +
                    "Les achats déjà verrouillés sont retournés inchangés.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achats verrouillés"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé (achat appartenant à un autre utilisateur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Au moins un achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<java.util.List<PurchaseLightResponse>> lockBulk(
            @Valid @RequestBody BulkPurchaseIdsRequest request) {
        var purchases = service.lockBulk(request.purchaseIds()).stream()
                .map(purchaseLightWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(purchases);
    }

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Déverrouiller plusieurs achats",
            description = "Opération en tout-ou-rien réservée au SUPER_ADMIN. " +
                    "Les achats non verrouillés sont retournés inchangés.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achats déverrouillés"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé (rôle SUPER_ADMIN requis)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Au moins un achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<java.util.List<PurchaseLightResponse>> unlockBulk(
            @Valid @RequestBody BulkPurchaseIdsRequest request) {
        var purchases = service.unlockBulk(request.purchaseIds()).stream()
                .map(purchaseLightWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(purchases);
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Déverrouiller un achat",
            description = "Déverrouille un achat précédemment verrouillé. Réservé au SUPER_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achat déverrouillé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé (rôle SUPER_ADMIN requis)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PurchaseResponse> unlock(
            @Parameter(description = "Identifiant de l'achat") @PathVariable UUID id) {
        Purchase unlocked = service.unlock(id);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(unlocked));
    }

    @PatchMapping("/{id}/budget")
    @PreAuthorize("hasAuthority('PERM_purchase:write')")
    @Operation(summary = "Réassigner un achat à un autre budget",
            description = "L'auteur de l'achat doit être éditeur de l'ancien et du nouveau budget.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achat réassigné"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé (non éditeur ou non auteur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Achat ou budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PurchaseResponse> reassignBudget(
            @Parameter(description = "Identifiant de l'achat") @PathVariable UUID id,
            @Valid @RequestBody ReassignPurchaseBudgetRequest request) {
        Purchase updated = service.reassignBudget(id, request.budgetId(), request.date(), request.raison());
        return ResponseEntity.ok(purchaseWebMapper.toResponse(updated));
    }

    @PatchMapping("/budget")
    @PreAuthorize("hasAuthority('PERM_purchase:write')")
    @Operation(summary = "Réassigner plusieurs achats à un même budget",
            description = "Opération en tout-ou-rien : si l'un des achats échoue (introuvable, non-auteur, " +
                    "non-éditeur d'un budget concerné), aucune réassignation n'est appliquée. " +
                    "Toutes les paires de mouvements de ledger générées partagent un même correlationId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Achats réassignés"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé (non éditeur ou non auteur)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Au moins un achat ou budget introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkReassignPurchaseBudgetResponse> reassignBudgetBulk(
            @Valid @RequestBody BulkReassignPurchaseBudgetRequest request) {
        BulkReassignBudgetResult result = service.reassignBudgetBulk(
                request.purchaseIds(), request.budgetId(), request.date(), request.raison());
        var purchases = result.purchases().stream()
                .map(purchaseLightWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(new BulkReassignPurchaseBudgetResponse(
                purchases.size(), result.correlationId(), purchases));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_purchase:import')")
    @Operation(summary = "Importer des achats depuis un fichier",
            description = "Importe des achats depuis un fichier .xlsx, .docx ou .csv. Le format est détecté à partir de l'extension.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import traité (succès et erreurs ligne par ligne)"),
            @ApiResponse(responseCode = "400", description = "Format de fichier non supporté",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PurchaseImportResponse importPurchases(
            @Parameter(description = "Fichier à importer (.xlsx, .docx, .csv)")
            @RequestPart("file") MultipartFile file) {
        ImportFormat format = detectFormat(file);
        try (var stream = file.getInputStream()) {
            PurchaseImportResult result = importService.importPurchases(stream, format);
            return purchaseImportWebMapper.toResponse(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostMapping(value = "/import/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_purchase:import')")
    @Operation(summary = "Valider un fichier d'import sans persistance",
            description = "Analyse le fichier et retourne les lignes valides (avec aperçu) et les lignes en erreur, sans rien enregistrer en base.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation effectuée"),
            @ApiResponse(responseCode = "400", description = "Format de fichier non supporté",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PurchaseImportValidationResponse validateImport(
            @Parameter(description = "Fichier à valider (.xlsx, .docx, .csv)")
            @RequestPart("file") MultipartFile file) {
        ImportFormat format = detectFormat(file);
        try (var stream = file.getInputStream()) {
            PurchaseImportValidationResult result = importService.validate(stream, format);
            return purchaseImportWebMapper.toValidationResponse(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ImportFormat detectFormat(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".xlsx")) return ImportFormat.EXCEL_XLSX;
            if (lower.endsWith(".docx")) return ImportFormat.WORD;
            if (lower.endsWith(".csv")) return ImportFormat.CSV;
        }
        throw new UnsupportedImportFormatException(name);
    }

}
