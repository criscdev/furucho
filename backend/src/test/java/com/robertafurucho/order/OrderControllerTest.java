package com.robertafurucho.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController.
 */
@SuppressWarnings("null")
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    /** Always-future date so @Future validation never flakes. */
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);
    private static final String FUTURE_DATE_STR = Objects.requireNonNull(FUTURE_DATE.toString());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_WithValidData_ReturnsCreated() throws Exception {
        // Arrange
        OrderResponse response = new OrderResponse(
            1L,
            "Maria da Silva",
            "maria@exemplo.com",
            "11999998888",
            "Rua das Flores, 123",
            "01234567",
            "Boneca de biscuit",
            "Cabelos castanhos, olhos verdes",
            FUTURE_DATE,
            LocalDateTime.now(),
            OrderStatus.PENDING
        );
        when(orderService.createOrder(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                    {
                        "name": "Maria da Silva",
                        "email": "maria@exemplo.com",
                        "phone": "11999998888",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Cabelos castanhos, olhos verdes",
                        "receiveDate": "%s"
                    }
                    """.formatted(FUTURE_DATE_STR)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Maria da Silva"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createOrder_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(("""
                    {
                        "name": "Maria da Silva",
                        "email": "invalid-email",
                        "phone": "11999998888",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Cabelos castanhos, olhos verdes",
                        "receiveDate": "%s"
                    }
                    """.formatted(FUTURE_DATE_STR))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void createOrder_WithMissingName_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                    {
                        "email": "maria@exemplo.com",
                        "phone": "11999998888",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Cabelos castanhos, olhos verdes",
                        "receiveDate": "%s"
                    }
                    """.formatted(FUTURE_DATE_STR)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getOrderById_WhenExists_ReturnsOrder() throws Exception {
        // Arrange
        OrderResponse response = new OrderResponse(
            1L,
            "Maria da Silva",
            "maria@exemplo.com",
            "11999998888",
            "Rua das Flores, 123",
            "01234567",
            "Boneca de biscuit",
            "Cabelos castanhos",
            FUTURE_DATE,
            LocalDateTime.now(),
            OrderStatus.PENDING
        );
        when(orderService.getOrderById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Maria da Silva"));
    }

    @Test
    void getOrderById_WhenNotExists_ReturnsNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Não encontrado"));
    }

    // -------------------------------------------------------------------------
    // Batch 2C — GET /api/orders
    // -------------------------------------------------------------------------

    @Test
    void getAllOrders_ReturnsListOfOrders() throws Exception {
        var response1 = new OrderResponse(1L, "Ana", "ana@x.com", "11999990001",
            "Rua A", "01234567", "Boneca", "Detalhe", FUTURE_DATE, LocalDateTime.now(), OrderStatus.PENDING);
        var response2 = new OrderResponse(2L, "Bia", "bia@x.com", "11999990002",
            "Rua B", "01234568", "Boneca", "Detalhe", FUTURE_DATE, LocalDateTime.now(), OrderStatus.CONFIRMED);
        when(orderService.getAllOrders()).thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
    }

    @Test
    void getAllOrders_WhenEmpty_ReturnsEmptyList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // -------------------------------------------------------------------------
    // Batch 2C — PATCH /api/orders/{id}/status
    // -------------------------------------------------------------------------

    @Test
    void updateOrderStatus_WithValidStatus_ReturnsUpdatedOrder() throws Exception {
        var updated = new OrderResponse(1L, "Ana", "ana@x.com", "11999990001",
            "Rua A", "01234567", "Boneca", "Detalhe", FUTURE_DATE, LocalDateTime.now(), OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(updated);

        mockMvc.perform(patch("/api/orders/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_WhenNotFound_ReturnsNotFound() throws Exception {
        when(orderService.updateOrderStatus(eq(404L), any()))
            .thenThrow(new OrderNotFoundException(404L));

        mockMvc.perform(patch("/api/orders/404/status").param("status", "CONFIRMED"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Não encontrado"));
    }

    @Test
    void updateOrderStatus_WithInvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status").param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Parâmetro inválido"));
    }

    // -------------------------------------------------------------------------
    // Batch 2C — Additional constraint validations
    // -------------------------------------------------------------------------

    @Test
    void createOrder_WithPastDate_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                    {
                        "name": "Maria da Silva",
                        "email": "maria@exemplo.com",
                        "phone": "11999998888",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Descrição válida",
                        "receiveDate": "2020-01-01"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.receiveDate").exists());
    }

    @Test
    void createOrder_WithInvalidPhone_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                    {
                        "name": "Maria da Silva",
                        "email": "maria@exemplo.com",
                        "phone": "119",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Descrição válida",
                        "receiveDate": "%s"
                    }
                    """.formatted(FUTURE_DATE_STR)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void createOrder_WithNameTooLong_ReturnsBadRequest() throws Exception {
        String longName = "A".repeat(201);
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                    {
                        "name": "%s",
                        "email": "maria@exemplo.com",
                        "phone": "11999998888",
                        "address": "Rua das Flores, 123",
                        "postalCode": "01234-567",
                        "orderScope": "Boneca de biscuit",
                        "orderScopeDetail": "Descrição válida",
                        "receiveDate": "%s"
                    }
                    """.formatted(longName, FUTURE_DATE_STR)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
