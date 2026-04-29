package ara.project.takalo.product.infrastructure.rest;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import ara.project.takalo.product.infrastructure.rest.mapper.ProductWebMapper;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Produits", description = "Gestion des produits")
public class ProductController {

    private final ProductServicePort productServicePort;
    private final ProductWebMapper webMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_product:write')")
    @Operation(summary = "Créer un produit")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produit créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Catégorie associée introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        var domain = webMapper.toDomain(request);
        var savedProduct = productServicePort.create(domain);
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(savedProduct));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_product:read')")
    @Operation(summary = "Obtenir un produit par identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit trouvé"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> getById(
            @Parameter(description = "Identifiant du produit") @PathVariable UUID id) {
        return ResponseEntity.ok(webMapper.toResponse(productServicePort.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_product:read')")
    @Operation(summary = "Lister tous les produits", description = "Liste paginée de l'ensemble des produits.")
    public ResponseEntity<PagedResponse<ProductResponse>> findAll(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {

        var pagedDomain = productServicePort.findAll(page, size);
        PagedResponse<ProductResponse> response = webMapper.toResponses(pagedDomain);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_product:read')")
    @Operation(summary = "Rechercher des produits",
            description = "Recherche paginée par nom partiel et/ou liste de catégories.")
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @Parameter(description = "Filtre partiel sur le nom") @RequestParam(required = false) String name,
            @Parameter(description = "Identifiants de catégories à inclure") @RequestParam(required = false) List<UUID> categoryIds,
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size) {

        PagedResponse<Product> pagedDomain = productServicePort.searchByCategoriesOrName(categoryIds, name, page, size);
        PagedResponse<ProductResponse> response = webMapper.toResponses(pagedDomain);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_product:write')")
    @Operation(summary = "Mettre à jour un produit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produit mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produit ou catégorie introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ProductResponse update(
            @Parameter(description = "Identifiant du produit") @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        Product toUpdate = new Product(id,
                request.name(),
                request.categoryId(),
                null,
                null);
        Product updated = productServicePort.update(id, toUpdate);
        return webMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_product:write')")
    @Operation(summary = "Supprimer un produit")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produit supprimé"),
            @ApiResponse(responseCode = "404", description = "Produit introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(@Parameter(description = "Identifiant du produit") @PathVariable UUID id) {
        productServicePort.delete(id);
    }
}
