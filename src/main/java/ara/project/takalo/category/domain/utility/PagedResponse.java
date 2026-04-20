package ara.project.takalo.category.domain.utility;

import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(List<T> content,
                               int pageNumber,
                               int pageSize,
                               long totalElements,
                               int totalPages,
                               boolean isLast) {

    public <R> PagedResponse<R> map(Function<T, R> converter) {
        List<R> mappedContent = this.content.stream()
                .map(converter)
                .toList();

        return new PagedResponse<>(
                mappedContent,
                this.pageNumber,
                this.pageSize,
                this.totalElements,
                this.totalPages,
                this.isLast
        );
    }
}
