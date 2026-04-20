package ara.project.takalo.category.infrastructure.web.dto;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
}
