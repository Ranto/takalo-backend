package ara.project.takalo.verification.infrastructure.rest;

import ara.project.takalo.budget.application.port.in.BudgetServicePort;
import ara.project.takalo.budget.domain.model.Budget;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserServicePort;
import ara.project.takalo.user.application.port.out.PermissionRepository;
import ara.project.takalo.verification.application.port.in.VerificationServicePort;
import ara.project.takalo.verification.domain.model.DenominationCount;
import ara.project.takalo.verification.domain.model.RegularizationKind;
import ara.project.takalo.verification.domain.model.Verification;
import ara.project.takalo.verification.infrastructure.rest.mapper.VerificationWebMapper;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VerificationController.class)
@Import({VerificationWebMapper.class})
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationServicePort service;

    @MockitoBean
    private BudgetServicePort budgetService;

    @MockitoBean
    private UserServicePort userServicePort;

    @MockitoBean
    private PermissionRepository permissionRepository;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final UUID BUDGET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Verification sampleVerification() {
        return new Verification(
                UUID.randomUUID(), BUDGET_ID, UUID.randomUUID(),
                LocalDate.of(2026, 5, 20), null,
                new BigDecimal("12000.00"), new BigDecimal("12000.00"), BigDecimal.ZERO,
                List.of(new DenominationCount(10000, 1), new DenominationCount(1000, 2)),
                RegularizationKind.NONE, null, null,
                Instant.parse("2026-05-20T10:00:00Z"), UUID.randomUUID()
        );
    }

    private Budget sampleBudget() {
        return new Budget(BUDGET_ID, "Caisse", "", new BigDecimal("100000.00"),
                UUID.randomUUID(), Set.of(), Instant.now(), null);
    }

    @Test
    void post_returnsCreatedVerification() throws Exception {
        when(service.create(any())).thenReturn(sampleVerification());
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(sampleBudget());

        String body = """
                {
                  "budgetId": "%s",
                  "verificationDate": "2026-05-20",
                  "referenceRest": 12000.00,
                  "countedTotal": 12000.00,
                  "denominations": [
                    {"value": 10000, "quantity": 1},
                    {"value": 1000, "quantity": 2}
                  ]
                }
                """.formatted(BUDGET_ID);

        mockMvc.perform(post("/api/v1/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetId").value(BUDGET_ID.toString()))
                .andExpect(jsonPath("$.budgetName").value("Caisse"))
                .andExpect(jsonPath("$.regularizationKind").value("NONE"));
    }

    @Test
    void post_rejectsInvalidRequest() throws Exception {
        // missing required fields
        String body = "{}";
        mockMvc.perform(post("/api/v1/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returnsPagedResults() throws Exception {
        when(service.search(eq(BUDGET_ID), any(), any(), eq(0), eq(10)))
                .thenReturn(new PagedResponse<>(List.of(sampleVerification()), 0, 10, 1, 1, true));
        when(budgetService.getRawById(BUDGET_ID)).thenReturn(sampleBudget());

        mockMvc.perform(get("/api/v1/verifications")
                        .param("budgetId", BUDGET_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].budgetName").value("Caisse"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
