package ara.project.takalo.product.infrastructure.rest.mapper;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductWebMapperTest {

    @Mock
    private ProductCategoryServicePort categoryServicePort;

    @InjectMocks
    private ProductWebMapper mapper;

    @Test
    void toDomain_buildsDomainWithNullIdAndTimestamps() {
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = new ProductRequest("Phone", categoryId);

        Product domain = mapper.toDomain(request);

        assertThat(domain.id()).isNull();
        assertThat(domain.name()).isEqualTo("Phone");
        assertThat(domain.categoryId()).isEqualTo(categoryId);
        assertThat(domain.createdAt()).isNull();
        assertThat(domain.updatedAt()).isNull();
    }

    @Test
    void toResponse_withCategoryId_resolvesLabel() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product domain = new Product(id, "Phone", categoryId, null, null);
        when(categoryServicePort.getCategoryLabels(Set.of(categoryId))).thenReturn(Map.of(categoryId, "Books"));

        ProductResponse response = mapper.toResponse(domain);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Phone");
        assertThat(response.category().id()).isEqualTo(categoryId);
        assertThat(response.category().label()).isEqualTo("Books");
    }

    @Test
    void toResponse_withoutCategoryId_skipsLookupAndUsesNullLabel() {
        UUID id = UUID.randomUUID();
        Product domain = new Product(id, "Phone", null, null, null);

        ProductResponse response = mapper.toResponse(domain);

        assertThat(response.category().id()).isNull();
        assertThat(response.category().label()).isNull();
        verify(categoryServicePort, never()).getCategoryLabels(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toResponses_aggregatesCategoryIdsIntoSingleLookup() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Product first = new Product(p1, "A", c1, null, null);
        Product second = new Product(p2, "B", c2, null, null);
        Product third = new Product(p3, "C", null, null, null);
        PagedResponse<Product> page = new PagedResponse<>(List.of(first, second, third), 0, 10, 3L, 1, true);

        when(categoryServicePort.getCategoryLabels(Set.of(c1, c2)))
                .thenReturn(Map.of(c1, "Cat1", c2, "Cat2"));

        PagedResponse<ProductResponse> result = mapper.toResponses(page);

        assertThat(result.content()).hasSize(3);
        assertThat(result.content().get(0).category().label()).isEqualTo("Cat1");
        assertThat(result.content().get(1).category().label()).isEqualTo("Cat2");
        assertThat(result.content().get(2).category().id()).isNull();
        assertThat(result.content().get(2).category().label()).isNull();
        assertThat(result.pageNumber()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(3L);
    }

    @Test
    void toResponses_whenNoCategoryIds_skipsLookup() {
        Product p = new Product(UUID.randomUUID(), "A", null, null, null);
        PagedResponse<Product> page = new PagedResponse<>(List.of(p), 0, 10, 1L, 1, true);

        PagedResponse<ProductResponse> result = mapper.toResponses(page);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().category().label()).isNull();
        verify(categoryServicePort, never()).getCategoryLabels(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void toResponses_emptyPage_returnsEmptyAndSkipsLookup() {
        PagedResponse<Product> page = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);

        PagedResponse<ProductResponse> result = mapper.toResponses(page);

        assertThat(result.content()).isEmpty();
        verify(categoryServicePort, never()).getCategoryLabels(org.mockito.ArgumentMatchers.any());
    }
}
