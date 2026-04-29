package ara.project.takalo.product.infrastructure.rest;

import ara.project.takalo.product.application.port.in.ProductServicePort;
import ara.project.takalo.product.domain.model.Product;
import ara.project.takalo.product.infrastructure.rest.dto.ProductCategoryInfo;
import ara.project.takalo.product.infrastructure.rest.dto.ProductRequest;
import ara.project.takalo.product.infrastructure.rest.dto.ProductResponse;
import ara.project.takalo.product.infrastructure.rest.mapper.ProductWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.user.application.port.in.UserServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductServicePort service;

    @MockitoBean
    private ProductWebMapper webMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private UserServicePort userServicePort;

    @Test
    void create_returns201AndPassesMappedDomain() throws Exception {
        UUID generated = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = new ProductRequest("Phone", categoryId);
        Product mappedDomain = new Product(null, "Phone", categoryId, null, null);
        Product saved = new Product(generated, "Phone", categoryId, null, null);
        ProductResponse response = new ProductResponse(generated, "Phone", new ProductCategoryInfo(categoryId, "Books"));

        when(webMapper.toDomain(any(ProductRequest.class))).thenReturn(mappedDomain);
        when(service.create(mappedDomain)).thenReturn(saved);
        when(webMapper.toResponse(saved)).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generated.toString()))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.category.label").value("Books"));
    }

    @Test
    void create_whenNameBlank_returns400() throws Exception {
        ProductRequest request = new ProductRequest("", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenNameTooShort_returns400() throws Exception {
        ProductRequest request = new ProductRequest("ab", UUID.randomUUID());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Product domain = new Product(id, "Phone", null, null, null);
        ProductResponse response = new ProductResponse(id, "Phone", new ProductCategoryInfo(null, null));

        when(service.getById(id)).thenReturn(domain);
        when(webMapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Phone"));
    }

    @Test
    void findAll_withDefaults_callsServiceWithDefaultParams() throws Exception {
        PagedResponse<Product> domainPage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        PagedResponse<ProductResponse> responsePage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);

        when(service.findAll(0, 10)).thenReturn(domainPage);
        when(webMapper.toResponses(domainPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(service).findAll(0, 10);
    }

    @Test
    void findAll_withParams_passesPageAndSize() throws Exception {
        PagedResponse<Product> domainPage = new PagedResponse<>(List.of(), 2, 5, 0L, 0, true);
        PagedResponse<ProductResponse> responsePage = new PagedResponse<>(List.of(), 2, 5, 0L, 0, true);

        when(service.findAll(2, 5)).thenReturn(domainPage);
        when(webMapper.toResponses(domainPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/products").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(service).findAll(2, 5);
    }

    @Test
    void search_passesParamsToService() throws Exception {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        PagedResponse<Product> domainPage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        PagedResponse<ProductResponse> responsePage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);

        when(service.searchByCategoriesOrName(eq(List.of(c1, c2)), eq("Pho"), eq(1), eq(20)))
                .thenReturn(domainPage);
        when(webMapper.toResponses(domainPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/products/search")
                        .param("name", "Pho")
                        .param("categoryIds", c1.toString(), c2.toString())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(service).searchByCategoriesOrName(List.of(c1, c2), "Pho", 1, 20);
    }

    @Test
    void search_withoutParams_usesDefaults() throws Exception {
        PagedResponse<Product> domainPage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);
        PagedResponse<ProductResponse> responsePage = new PagedResponse<>(List.of(), 0, 10, 0L, 0, true);

        when(service.searchByCategoriesOrName(eq(null), eq(null), eq(0), eq(10)))
                .thenReturn(domainPage);
        when(webMapper.toResponses(domainPage)).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/products/search"))
                .andExpect(status().isOk());

        verify(service).searchByCategoriesOrName(null, null, 0, 10);
    }

    @Test
    void update_returns200AndUsesPathId() throws Exception {
        UUID pathId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequest request = new ProductRequest("New", categoryId);
        Product saved = new Product(pathId, "New", categoryId, null, null);
        ProductResponse response = new ProductResponse(pathId, "New", new ProductCategoryInfo(categoryId, "Cat"));

        when(service.update(eq(pathId), any(Product.class))).thenReturn(saved);
        when(webMapper.toResponse(saved)).thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{id}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pathId.toString()))
                .andExpect(jsonPath("$.name").value("New"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(service).update(eq(pathId), captor.capture());
        Product passed = captor.getValue();
        assertThat(passed.id()).isEqualTo(pathId);
        assertThat(passed.name()).isEqualTo("New");
        assertThat(passed.categoryId()).isEqualTo(categoryId);
        assertThat(passed.createdAt()).isNull();
        assertThat(passed.updatedAt()).isNull();
    }

    @Test
    void update_whenNameBlank_returns400() throws Exception {
        UUID pathId = UUID.randomUUID();
        ProductRequest request = new ProductRequest("", UUID.randomUUID());

        mockMvc.perform(put("/api/v1/products/{id}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }
}
