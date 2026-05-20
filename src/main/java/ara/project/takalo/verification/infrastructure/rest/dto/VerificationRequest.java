package ara.project.takalo.verification.infrastructure.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VerificationRequest(
        @NotNull(message = "Le budget est obligatoire.")
        UUID budgetId,

        @NotNull(message = "La date de vérification est obligatoire.")
        @PastOrPresent(message = "La date de vérification doit être passée ou aujourd'hui.")
        LocalDate verificationDate,

        @Size(max = 2000, message = "La note ne doit pas dépasser 2000 caractères.")
        String note,

        @NotNull(message = "Le reste de référence est obligatoire.")
        @PositiveOrZero(message = "Le reste de référence doit être positif ou nul.")
        BigDecimal referenceRest,

        @NotNull(message = "Le total compté est obligatoire.")
        @PositiveOrZero(message = "Le total compté doit être positif ou nul.")
        BigDecimal countedTotal,

        @NotEmpty(message = "La liste des coupures est obligatoire.")
        @Valid
        List<DenominationCountRequest> denominations,

        @Valid
        RegularizationRequest regularization
) {
}
