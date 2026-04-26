package ara.project.takalo.category.infrastructure.rest;

import ara.project.takalo.category.application.port.in.ProductCategoryServicePort;
import ara.project.takalo.category.domain.model.ProductCategory;
import ara.project.takalo.category.infrastructure.rest.dto.ProductCategoryRequest;
import ara.project.takalo.category.infrastructure.rest.mapper.ProductCategoryWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductCategoryController.class)
@Import(ProductCategoryWebMapper.class)
class ProductCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductCategoryServicePort service;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void create_returns201AndPassesDomainWithNullId() throws Exception {
        UUID generated = UUID.randomUUID();
        ProductCategoryRequest request = new ProductCategoryRequest("Books", "All books");
        when(service.create(any(ProductCategory.class)))
                .thenReturn(new ProductCategory(generated, "Books", "All books"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generated.toString()))
                .andExpect(jsonPath("$.label").value("Books"))
                .andExpect(jsonPath("$.description").value("All books"));

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().id()).isNull();
        assertThat(captor.getValue().label()).isEqualTo("Books");
    }

    @Test
    void getById_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenReturn(new ProductCategory(id, "Books", "desc"));

        mockMvc.perform(get("/api/v1/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.label").value("Books"));
    }

    @Test
    void search_withParams_passesThemToServiceAndMapsResponse() throws Exception {
        UUID id = UUID.randomUUID();
        PagedResponse<ProductCategory> page = new PagedResponse<>(
                List.of(new ProductCategory(id, "Books", "desc")), 2, 5, 1L, 1, true);
        when(service.search("foo", 2, 5)).thenReturn(page);

        mockMvc.perform(get("/api/v1/categories")
                        .param("label", "foo")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].label").value("Books"))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_withoutParams_usesDefaults() throws Exception {
        when(service.search(eq(null), eq(0), eq(10)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 10, 0L, 0, true));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());

        verify(service).search(null, 0, 10);
    }

    @Test
    void update_returns200AndUsesPathId() throws Exception {
        UUID pathId = UUID.randomUUID();
        ProductCategoryRequest request = new ProductCategoryRequest("New", "new desc");
        when(service.update(eq(pathId), any(ProductCategory.class)))
                .thenReturn(new ProductCategory(pathId, "New", "new desc"));

        mockMvc.perform(put("/api/v1/categories/{id}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pathId.toString()))
                .andExpect(jsonPath("$.label").value("New"));

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(service).update(eq(pathId), captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(pathId);
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }
}
