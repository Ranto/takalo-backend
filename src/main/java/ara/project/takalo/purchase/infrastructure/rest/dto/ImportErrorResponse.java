package ara.project.takalo.purchase.infrastructure.rest.dto;

public record ImportErrorResponse(Integer lineNumber, String rawValue, String errorMessage) {
}