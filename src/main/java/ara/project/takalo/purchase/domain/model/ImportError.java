package ara.project.takalo.purchase.domain.model;

public record ImportError(Integer lineNumber, String rawValue, String errorMessage) {
}
