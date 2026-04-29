package ara.project.takalo.budget.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.mapper.BudgetWebMapper;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.user.application.port.in.UserServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@Import(BudgetWebMapper.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BudgetServicePort service;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private UserServicePort userServicePort;

    @Test
    void create_returns201AndBody() throws Exception {
        UUID generated = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        Budget saved = new Budget(generated, "Courses", "desc",
                new BigDecimal("500.00"), creator, Set.of(creator),
                Instant.parse("2024-01-01T00:00:00Z"), null);

        when(service.create(any(Budget.class))).thenReturn(saved);

        String json = """
                { "name": "Courses", "description": "desc", "fond": 500.00 }
                """;

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generated.toString()))
                .andExpect(jsonPath("$.name").value("Courses"))
                .andExpect(jsonPath("$.fond").value(500.00))
                .andExpect(jsonPath("$.totalAchats").value(0))
                .andExpect(jsonPath("$.reste").value(500.00))
                .andExpect(jsonPath("$.createdBy").value(creator.toString()));
    }

    @Test
    void create_whenNameBlank_returns400() throws Exception {
        String json = """
                { "name": "  ", "fond": 100.00 }
                """;

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenFondMissing_returns400() throws Exception {
        String json = """
                { "name": "X" }
                """;

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenNameAlreadyExists_returns409() throws Exception {
        when(service.create(any(Budget.class)))
                .thenThrow(new AlreadyExistsException("Un budget avec ce nom existe déjà"));

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Dup", "fond": 100.00 }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_returns200WithComputedReste() throws Exception {
        UUID id = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("100.00"),
                creator, Set.of(creator), Instant.now(), null);
        BudgetWithBalance bwb = new BudgetWithBalance(budget, new BigDecimal("30.00"));
        when(service.getById(id)).thenReturn(bwb);

        mockMvc.perform(get("/api/v1/budgets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fond").value(100.00))
                .andExpect(jsonPath("$.totalAchats").value(30.00))
                .andExpect(jsonPath("$.reste").value(70.00));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenThrow(new ResourceNotFoundException("Budget non trouvé"));

        mockMvc.perform(get("/api/v1/budgets/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_negativeReste_isReturnedAsNegative() throws Exception {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        BudgetWithBalance bwb = new BudgetWithBalance(budget, new BigDecimal("150.00"));
        when(service.getById(id)).thenReturn(bwb);

        mockMvc.perform(get("/api/v1/budgets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reste").value(-50.00));
    }

    @Test
    void list_returnsPagedLightResponses() throws Exception {
        UUID id = UUID.randomUUID();
        Budget budget = new Budget(id, "B", null, new BigDecimal("100.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
        BudgetWithBalance bwb = new BudgetWithBalance(budget, new BigDecimal("25.00"));
        PagedResponse<BudgetWithBalance> page = new PagedResponse<>(List.of(bwb), 0, 10, 1L, 1, true);
        when(service.findAll(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].fond").value(100.00))
                .andExpect(jsonPath("$.content[0].reste").value(75.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
