package ara.project.takalo.budget.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.application.port.in.BudgetUpdateCommand;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.budget.domain.model.BudgetMovement;
import ara.project.takalo.budget.domain.model.BudgetMovementSource;
import ara.project.takalo.budget.domain.model.BudgetMovementType;
import ara.project.takalo.budget.domain.model.BudgetWithBalance;
import ara.project.takalo.budget.infrastructure.rest.mapper.BudgetWebMapper;
import ara.project.takalo.shared.domain.exception.AlreadyExistsException;
import ara.project.takalo.shared.domain.exception.ForbiddenException;
import ara.project.takalo.shared.domain.exception.ResourceNotFoundException;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.user.application.port.in.UserServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @ParameterizedTest(name = "missing {0} returns 400 with field error on \"{0}\"")
    @CsvSource({
            "date,   '{ \"montant\": 100.00, \"raison\": \"Remboursement\" }'",
            "raison, '{ \"montant\": 100.00, \"date\": \"2026-04-15T09:00:00Z\" }'"
    })
    void postCredit_missingDateOrReason_400(String missingField, String json) throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/budgets/{id}/credits", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + missingField).exists());
    }

    @ParameterizedTest(name = "missing {0} returns 400 with field error on \"{0}\"")
    @CsvSource({
            "date,   '{ \"sourceBudgetId\": \"11111111-1111-1111-1111-111111111111\", \"targetBudgetId\": \"22222222-2222-2222-2222-222222222222\", \"montant\": 50.00, \"raison\": \"Réallocation\" }'",
            "raison, '{ \"sourceBudgetId\": \"11111111-1111-1111-1111-111111111111\", \"targetBudgetId\": \"22222222-2222-2222-2222-222222222222\", \"montant\": 50.00, \"date\": \"2026-04-29T10:00:00Z\" }'"
    })
    void postTransfer_missingDateOrReason_400(String missingField, String json) throws Exception {
        mockMvc.perform(post("/api/v1/budgets/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + missingField).exists());
    }

    @Test
    void deleteBudget_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/budgets/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    void deleteBudget_whenNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget non trouvé"))
                .when(service).delete(id);

        mockMvc.perform(delete("/api/v1/budgets/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Restriction de modification aux éditeurs (S39 / S40 / S41)
    // ------------------------------------------------------------------

    @Test
    void update_whenServiceThrowsForbidden_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(org.mockito.ArgumentMatchers.eq(id), any(BudgetUpdateCommand.class)))
                .thenThrow(new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget"));

        mockMvc.perform(put("/api/v1/budgets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Vacances" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void deleteBudget_whenServiceThrowsForbidden_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ForbiddenException("Vous n'êtes pas autorisé à modifier ce budget"))
                .when(service).delete(id);

        mockMvc.perform(delete("/api/v1/budgets/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void movements_authenticatedReader_returns200_evenWhenNotEditor() throws Exception {
        UUID id = UUID.randomUUID();
        BudgetMovement credit = new BudgetMovement(UUID.randomUUID(), id,
                BudgetMovementType.CREDIT_EXTERNE, new BigDecimal("200.00"),
                Instant.parse("2026-04-15T09:00:00Z"), "Cadeau",
                null, null, BudgetMovementSource.INCONNUE, null, null, null);
        when(service.findMovements(id, null)).thenReturn(List.of(credit));

        mockMvc.perform(get("/api/v1/budgets/{id}/movements", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("CREDIT_EXTERNE"))
                .andExpect(jsonPath("$[0].source").value("INCONNUE"))
                .andExpect(jsonPath("$[0].montant").value(200.00));
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
