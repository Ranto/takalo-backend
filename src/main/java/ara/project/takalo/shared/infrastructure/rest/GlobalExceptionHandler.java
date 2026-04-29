package ara.project.takalo.shared.infrastructure.rest;

import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.InvalidOperationException;
import ara.project.takalo.shared.infrastructure.rest.dto.ErrorResponse;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(
            responseCode = "404",
            description = "Ressource introuvable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "Erreur d'intégrité des données",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Erreur d'intégrité des données : vérifiez que les références (ex: ID catégorie) existent.";

        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "Erreur de validation des champs de la requête",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation des champs",
                LocalDateTime.now(),
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ApiResponse(
            responseCode = "409",
            description = "Conflit : la ressource existe déjà",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleAlreadyExists(AlreadyExistsException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponse(
            responseCode = "401",
            description = "Authentification requise",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentification requise",
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponse(
            responseCode = "403",
            description = "Permission insuffisante",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Permission insuffisante",
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidOperationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "Opération invalide",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponse(
            responseCode = "403",
            description = "Accès interdit",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UnsupportedImportFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(
            responseCode = "400",
            description = "Format d'import non supporté",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ErrorResponse handleUnsupportedImportFormat(UnsupportedImportFormatException ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }
}
