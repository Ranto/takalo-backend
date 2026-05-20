package ara.project.takalo.verification.infrastructure.rest.dto;

import ara.project.takalo.verification.domain.model.RegularizationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegularizationRequest(
        @NotNull(message = "Le type de régularisation est obligatoire.")
        RegularizationKind kind,

        @NotBlank(message = "Le libellé de régularisation est obligatoire.")
        @Size(max = 255, message = "Le libellé ne doit pas dépasser 255 caractères.")
        String label,

        @NotNull(message = "La date de régularisation est obligatoire.")
        @PastOrPresent(message = "La date de régularisation doit être passée ou aujourd'hui.")
        LocalDate date
) {
}
