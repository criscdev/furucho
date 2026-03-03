package com.robertafurucho.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the full order lifecycle: POST → GET → PATCH → GET.
 *
 * <p>Uses {@code @SpringBootTest} with a real H2 database and full Spring context.
 * Each test starts with a clean database via {@code @BeforeEach} to ensure isolation.
 *
 * <p>Servlet filters are excluded via {@code addFilters = false} so the
 * {@link com.robertafurucho.config.RateLimitingFilter} does not interfere with
 * multi-POST test flows. Rate limiting is fully covered in its own test class.
 *
 * @decision Full-stack integration test (controller → service → repository → H2)
 *           validates that the wiring is correct end-to-end; unit tests in
 *           {@link OrderServiceTest} and {@link OrderControllerTest} cover
 *           individual layers in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Order Integration (POST→GET→PATCH→GET)")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    private String validOrderJson() {
        return """
            {
                "name": "Maria Silva",
                "email": "maria@example.com",
                "phone": "11999990000",
                "address": "Rua das Bonecas, 42",
                "postalCode": "01234-567",
                "orderScope": "Boneca de biscuit artesanal",
                "orderScopeDetail": "Boneca de biscuit com vestido azul e cabelo castanho, 20cm",
                "receiveDate": "%s"
            }
            """.formatted(FUTURE_DATE.toString());
    }

    @Test
    @DisplayName("full lifecycle: create → get by id → update status → verify update")
    void fullLifecycle_CreateGetPatchGet() throws Exception {
        // 1. POST /api/orders → 201 Created
        String createResponse = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Maria Silva"))
            .andExpect(jsonPath("$.email").value("maria@example.com"))
            .andExpect(jsonPath("$.phone").value("11999990000"))
            .andExpect(jsonPath("$.postalCode").value("01234567"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        // 2. GET /api/orders/{id} → 200 OK with same data
        mockMvc.perform(get("/api/orders/{id}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.name").value("Maria Silva"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        // 3. PATCH /api/orders/{id}/status?status=CONFIRMED → 200 OK
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 4. GET /api/orders/{id} → 200 OK with updated status
        mockMvc.perform(get("/api/orders/{id}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/orders returns all orders, newest first")
    void getAllOrders_ReturnsListNewestFirst() throws Exception {
        // Create two orders
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated());

        String secondOrderJson = """
            {
                "name": "João Santos",
                "email": "joao@example.com",
                "phone": "21888880000",
                "address": "Av. Brasil, 100",
                "postalCode": "20000-000",
                "orderScope": "Boneca de biscuit noivinha",
                "orderScopeDetail": "Boneca pequena de biscuit com véu branco",
                "receiveDate": "%s"
            }
            """.formatted(FUTURE_DATE.toString());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondOrderJson))
            .andExpect(status().isCreated());

        // GET all
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("João Santos"))
            .andExpect(jsonPath("$[1].name").value("Maria Silva"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns 404 for non-existent order")
    void getById_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Não encontrado"));
    }

    @Test
    @DisplayName("POST /api/orders with invalid body returns 400 with field errors")
    void createOrder_InvalidBody_Returns400() throws Exception {
        String invalidJson = """
            {
                "name": "",
                "email": "not-an-email",
                "phone": "123",
                "address": "",
                "postalCode": "abc",
                "orderScope": "",
                "orderScopeDetail": "",
                "receiveDate": "2020-01-01"
            }
            """;

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors").exists())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    @DisplayName("PATCH with invalid status returns 400")
    void updateStatus_InvalidStatus_Returns400() throws Exception {
        // Create an order first
        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                .param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("data persists across requests (H2 real database)")
    void dataPersists_AcrossRequests() throws Exception {
        // Create order
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated());

        // Verify it persists in the repository
        assertDatabaseOrderCount(1);

        // Create another
        String secondJson = """
            {
                "name": "Ana Costa",
                "email": "ana@example.com",
                "phone": "31777770000",
                "address": "Rua Y, 10",
                "postalCode": "30000-000",
                "orderScope": "Boneca de biscuit bailarina",
                "orderScopeDetail": "Boneca de biscuit bailarina rosa",
                "receiveDate": "%s"
            }
            """.formatted(FUTURE_DATE.toString());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondJson))
            .andExpect(status().isCreated());

        assertDatabaseOrderCount(2);
    }

    private void assertDatabaseOrderCount(int expected) {
        org.assertj.core.api.Assertions.assertThat(orderRepository.count()).isEqualTo(expected);
    }
}
