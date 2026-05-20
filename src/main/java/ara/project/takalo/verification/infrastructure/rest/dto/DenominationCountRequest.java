package ara.project.takalo.verification.infrastructure.rest.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record DenominationCountRequest(
        @Positive(message = "La valeur d'une coupure doit être strictement positive.")
        int value,

        @PositiveOrZero(message = "La quantité d'une coupure doit être positive ou nulle.")
        int quantity
) {
}
