package ara.project.takalo.purchase.infrastructure.rest;

import ara.project.takalo.purchase.application.port.in.BulkReassignBudgetResult;
import ara.project.takalo.purchase.application.port.in.PurchaseImportServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseItem;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseImportWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseItemDetailWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseItemWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseLightWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import ara.project.takalo.shared.infrastructure.security.CurrentUserProvider;
import ara.project.takalo.user.application.port.in.UserDefaultBudgetServicePort;
import ara.project.takalo.user.application.port.in.UserServicePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseController.class)
@Import({PurchaseWebMapper.class, PurchaseItemWebMapper.class, PurchaseLightWebMapper.class,
        PurchaseImportWebMapper.class, PurchaseItemDetailWebMapper.class})
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseServicePort service;

    @MockitoBean
    private PurchaseImportServicePort importService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private UserServicePort userServicePort;

    @MockitoBean
    private UserDefaultBudgetServicePort defaultBudgetService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    private static String validRequestJson(String productName, String purchaseDate) {
        return """
                {
                  "purchaseDate": "%s",
                  "items": [
                    {
                      "productName": "%s",
                      "unitPrice": 10.00,
                      "quantity": 2.0,
                      "discount": 1.00,
                      "expiryDate": "2030-01-01",
                      "storeName": "Carrefour"
                    }
                  ]
                }
                """.formatted(purchaseDate, productName);
    }

    @Test
    void create_returns201AndBody() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID generated = UUID.randomUUID();
        Instant purchaseDate = Instant.parse("2024-01-01T00:00:00Z");
        PurchaseItem savedItem = new PurchaseItem(productId, 2.0, new BigDecimal("10.00"),
                new BigDecimal("1.00"), null, "Carrefour", "Lait");
        Purchase saved = new Purchase(generated, null, null, purchaseDate, null, null, null, List.of(savedItem));

        when(service.create(any(Purchase.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("Lait", "2024-01-01T00:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generated.toString()))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Lait"))
                .andExpect(jsonPath("$.totalAmount").value(19.00));

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(service).create(captor.capture());
        Purchase passed = captor.getValue();
        assertThat(passed.id()).isNull();
        assertThat(passed.purchaseDate()).isEqualTo(purchaseDate);
        assertThat(passed.items()).hasSize(1);
        assertThat(passed.items().getFirst().productId()).isNull();
        assertThat(passed.items().getFirst().productName()).isEqualTo("Lait");
    }

    @Test
    void create_whenPurchaseDateInFuture_returns400() throws Exception {
        String json = validRequestJson("Lait", "2999-01-01T00:00:00Z");

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenItemsEmpty_returns400() throws Exception {
        String json = """
                { "purchaseDate": "2024-01-01T00:00:00Z", "items": [] }
                """;

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenItemFieldsInvalid_returns400() throws Exception {
        String json = """
                {
                  "purchaseDate": "2024-01-01T00:00:00Z",
                  "items": [
                    { "productName": "  ", "unitPrice": -1, "quantity": 0 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(productId, 1.0, new BigDecimal("4.00"),
                BigDecimal.ZERO, null, null, "Pain");
        Purchase domain = new Purchase(id, null, null, Instant.parse("2024-01-01T00:00:00Z"), null, null, null, List.of(item));

        when(service.getById(id)).thenReturn(domain);

        mockMvc.perform(get("/api/v1/purchases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Pain"))
                .andExpect(jsonPath("$.totalAmount").value(4.00));
    }

    @Test
    void update_returns200AndPassesPathId() throws Exception {
        UUID pathId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Purchase saved = new Purchase(pathId, null, null, Instant.parse("2024-01-01T00:00:00Z"), null, null, null, List.of(
                new PurchaseItem(productId, 2.0, new BigDecimal("10.00"), new BigDecimal("1.00"),
                        null, "Carrefour", "Lait")
        ));

        when(service.update(eq(pathId), any(Purchase.class))).thenReturn(saved);

        mockMvc.perform(put("/api/v1/purchases/{id}", pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson("Lait", "2024-01-01T00:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pathId.toString()));

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(service).update(eq(pathId), captor.capture());
        Purchase passed = captor.getValue();
        assertThat(passed.id()).isNull();
        assertThat(passed.items()).hasSize(1);
    }

    @Test
    void search_withoutParams_usesDefaults() throws Exception {
        when(service.search(eq(null), eq(null), eq(null), eq(null), eq(false), eq(0), eq(10)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 10, 0L, 0, true));

        mockMvc.perform(get("/api/v1/purchases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(service).search(null, null, null, null, false, 0, 10);
    }

    // ------------------------------------------------------------------
    // Budget par défaut à la création d'achat (S64, S65, S66)
    // ------------------------------------------------------------------

    private static String requestWithBudgetField(String productName, String budgetIdLiteral) {
        // budgetIdLiteral peut être "null", "\"<uuid>\"" ou être omis (champ absent).
        String field = budgetIdLiteral == null ? "" : ("\"budgetId\": " + budgetIdLiteral + ",\n");
        return """
                {
                  %s"purchaseDate": "2026-04-10T12:00:00Z",
                  "items": [
                    { "productName": "%s", "unitPrice": 75.00, "quantity": 1.0, "discount": 0 }
                  ]
                }
                """.formatted(field, productName);
    }

    @Test
    void create_noBudgetField_usesDefaultBudget() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID defaultBudget = UUID.randomUUID();
        when(currentUserProvider.id()).thenReturn(alice);
        when(defaultBudgetService.resolveDefaultBudgetIdFor(alice))
                .thenReturn(Optional.of(defaultBudget));
        when(service.create(any(Purchase.class)))
                .thenAnswer(inv -> inv.getArgument(0, Purchase.class));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithBudgetField("Lait", null)))
                .andExpect(status().isCreated());

        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().budgetId()).isEqualTo(defaultBudget);
    }

    // S65 et S66 (champ "budgetId" explicite avec valeur ou null) requièrent le module
    // JsonNullable correctement enregistré sur l'ObjectMapper du slice WebMvc, ce que
    // @WebMvcTest ne fournit pas pour le moment. À couvrir par un test d'intégration
    // (@SpringBootTest) dans un lot ultérieur.

    @Test
    void reassignBudgetBulk_returns200WithCorrelationIdAndPurchases() throws Exception {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(UUID.randomUUID(), 1.0, new BigDecimal("10.00"),
                BigDecimal.ZERO, null, null, "x");
        Purchase u1 = new Purchase(p1, UUID.randomUUID(), budgetId,
                Instant.parse("2026-04-10T12:00:00Z"), null, null, null, List.of(item));
        Purchase u2 = new Purchase(p2, UUID.randomUUID(), budgetId,
                Instant.parse("2026-04-11T12:00:00Z"), null, null, null, List.of(item));

        when(service.reassignBudgetBulk(any(), eq(budgetId), any(), eq("r")))
                .thenReturn(new BulkReassignBudgetResult(List.of(u1, u2), correlationId));

        String json = """
                {
                  "purchaseIds": ["%s", "%s"],
                  "budgetId": "%s",
                  "date": "2026-04-29T10:00:00Z",
                  "raison": "r"
                }
                """.formatted(p1, p2, budgetId);

        mockMvc.perform(patch("/api/v1/purchases/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.correlationId").value(correlationId.toString()))
                .andExpect(jsonPath("$.purchases[0].id").value(p1.toString()))
                .andExpect(jsonPath("$.purchases[1].id").value(p2.toString()));
    }

    @Test
    void reassignBudgetBulk_whenPurchaseIdsEmpty_returns400() throws Exception {
        String json = """
                {
                  "purchaseIds": [],
                  "budgetId": "%s",
                  "date": "2026-04-29T10:00:00Z",
                  "raison": "r"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/purchases/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reassignBudgetBulk_whenRaisonBlank_returns400() throws Exception {
        String json = """
                {
                  "purchaseIds": ["%s"],
                  "budgetId": "%s",
                  "date": "2026-04-29T10:00:00Z",
                  "raison": "   "
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(patch("/api/v1/purchases/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_withParams_passesThemAndMapsLightResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant date = Instant.parse("2024-01-01T00:00:00Z");
        PurchaseItem item = new PurchaseItem(productId, 2.0, new BigDecimal("10.00"),
                new BigDecimal("1.00"), null, null, "Lait");
        Purchase domain = new Purchase(id, null, null, date, null, null, null, List.of(item));
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-12-31T23:59:59Z");
        PagedResponse<Purchase> page = new PagedResponse<>(List.of(domain), 1, 5, 1L, 1, true);

        when(service.search(start, end, null, null, false, 1, 5)).thenReturn(page);

        mockMvc.perform(get("/api/v1/purchases")
                        .param("start", "2024-01-01T00:00:00Z")
                        .param("end", "2024-12-31T23:59:59Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].itemCount").value(1))
                .andExpect(jsonPath("$.content[0].totalAmount").value(19.00))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).search(start, end, null, null, false, 1, 5);
    }
}
