package ara.project.takalo.user.infrastructure.rest;

import ara.project.takalo.user.application.port.in.RoleServicePort;
import ara.project.takalo.user.infrastructure.rest.dto.RoleResponse;
import ara.project.takalo.user.infrastructure.rest.mapper.UserWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Rôles", description = "Référentiel des rôles RBAC")
public class RoleController {

    private final RoleServicePort roleServicePort;
    private final UserWebMapper webMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_user:manage')")
    @Operation(summary = "Lister les rôles disponibles")
    public ResponseEntity<List<RoleResponse>> findAll() {
        var roles = roleServicePort.findAll().stream().map(webMapper::toResponse).toList();
        return ResponseEntity.ok(roles);
    }
}
