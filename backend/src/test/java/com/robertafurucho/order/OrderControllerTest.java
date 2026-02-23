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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
            "Boneca de pano",
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
                        "orderScope": "Boneca de pano",
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
                        "orderScope": "Boneca de pano",
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
                        "orderScope": "Boneca de pano",
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
            "Boneca de pano",
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
        // Arrange
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/orders/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Não encontrado"));
    }
}
