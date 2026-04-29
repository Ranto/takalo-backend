package ara.project.takalo.user.infrastructure.rest;

import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.infrastructure.rest.dto.AssignRoleRequest;
import ara.project.takalo.user.infrastructure.rest.dto.UserResponse;
import ara.project.takalo.user.infrastructure.rest.mapper.UserWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs et de leurs rôles RBAC")
public class UserController {

    private final UserServicePort userServicePort;
    private final UserWebMapper webMapper;

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur courant")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var user = userServicePort.getByExternalId(jwt.getSubject());
        return ResponseEntity.ok(webMapper.toResponse(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_user:manage')")
    @Operation(summary = "Obtenir un utilisateur par identifiant")
    public ResponseEntity<UserResponse> getById(@Parameter(description = "Identifiant utilisateur") @PathVariable UUID id) {
        return ResponseEntity.ok(webMapper.toResponse(userServicePort.getById(id)));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('PERM_user:manage')")
    @Operation(summary = "Assigner un rôle à un utilisateur")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request) {
        var user = userServicePort.assignRole(id, request.roleName());
        return ResponseEntity.ok(webMapper.toResponse(user));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasAuthority('PERM_user:manage')")
    @Operation(summary = "Retirer un rôle à un utilisateur")
    public ResponseEntity<UserResponse> revokeRole(
            @PathVariable UUID id,
            @PathVariable String roleName) {
        var user = userServicePort.revokeRole(id, roleName);
        return ResponseEntity.ok(webMapper.toResponse(user));
    }
}
