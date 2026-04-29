package ara.project.takalo.user.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank(message = "Le nom du rôle est obligatoire") String roleName
) {
}
