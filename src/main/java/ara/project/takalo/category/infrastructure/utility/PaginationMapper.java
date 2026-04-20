package ara.project.takalo.category.infrastructure.utility;

import ara.project.takalo.category.domain.utility.PagedResponse;
import org.springframework.data.domain.Page;

import java.util.function.Function;

public class PaginationMapper {

    public static <S, T> PagedResponse<T> toPagedResponse(Page<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
