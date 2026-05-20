package ara.project.takalo.verification.infrastructure.rest;

import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.verification.application.port.in.VerificationServicePort;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.rest.dto.VerificationRequest;
import ara.project.takalo.verification.infrastructure.rest.dto.VerificationResponse;
import ara.project.takalo.verification.infrastructure.rest.mapper.VerificationWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Vérifications", description = "Comptage de caisse et régularisation")
public class VerificationController {

    private final VerificationServicePort service;
    private final VerificationWebMapper webMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une vérification de caisse",
            description = "Enregistre un comptage et, si demandé, crée automatiquement l'achat ou le crédit de régularisation correspondant.")
    public VerificationResponse create(@Valid @RequestBody VerificationRequest request) {
        Verification saved = service.create(webMapper.toCommand(request));
        return webMapper.toResponse(saved);
    }

    @GetMapping
    @Operation(summary = "Lister les vérifications de caisse")
    public PagedResponse<VerificationResponse> search(
            @RequestParam(required = false) UUID budgetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<Verification> result = service.search(budgetId, start, end, page, size);
        return result.map(webMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une vérification par identifiant")
    public VerificationResponse getById(@PathVariable UUID id) {
        return webMapper.toResponse(service.getById(id));
    }
}
