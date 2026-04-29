package ara.project.takalo.purchase.infrastructure.rest;

import ara.project.takalo.purchase.application.port.in.PurchaseImportServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import org.openapitools.jackson.nullable.JsonNullable;
import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseLightResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.ReassignPurchaseBudgetRequest;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseImportWebMapper;
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
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {
        PagedResponse<Purchase> result = service.search(start, end, page, size);
        return result.map(purchaseLightWebMapper::toResponse);
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
