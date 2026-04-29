package ara.project.takalo.category.infrastructure.rest;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryRequest;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryResponse;
import ara.project.takalo.category.infrastructure.rest.mapper.ProductCategoryWebMapper;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Catégories", description = "Gestion des catégories de produits")
public class ProductCategoryController {
    private final ProductCategoryServicePort service;
    private final ProductCategoryWebMapper webMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_category:manage')")
    @Operation(summary = "Créer une catégorie", description = "Crée une nouvelle catégorie de produit.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Catégorie créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Une catégorie avec ce libellé existe déjà",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductCategoryResponse create(@Valid @RequestBody ProductCategoryRequest request) {
        ProductCategory saved = service.create(webMapper.toDomain(request));
        return webMapper.toResponse(saved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_category:read')")
    @Operation(summary = "Obtenir une catégorie par identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégorie trouvée"),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductCategoryResponse getById(
            @Parameter(description = "Identifiant de la catégorie") @PathVariable UUID id) {
        return webMapper.toResponse(service.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_category:read')")
    @Operation(summary = "Rechercher des catégories",
            description = "Recherche paginée des catégories par libellé (filtre optionnel).")
    public PagedResponse<ProductCategoryResponse> search(
            @Parameter(description = "Filtre partiel sur le libellé") @RequestParam(required = false) String label,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {
        return service.search(label, page, size).map(webMapper::toResponse);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_category:manage')")
    @Operation(summary = "Mettre à jour une catégorie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégorie mise à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Une catégorie avec ce libellé existe déjà",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductCategoryResponse update(
            @Parameter(description = "Identifiant de la catégorie") @PathVariable UUID id,
            @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategory updated = service.update(id, webMapper.toDomain(id, request));
        return webMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_category:manage')")
    @Operation(summary = "Supprimer une catégorie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Catégorie supprimée"),
            @ApiResponse(responseCode = "404", description = "Catégorie introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@Parameter(description = "Identifiant de la catégorie") @PathVariable UUID id) {
        service.delete(id);
    }
}
