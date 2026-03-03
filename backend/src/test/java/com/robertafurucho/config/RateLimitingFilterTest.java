package com.robertafurucho.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robertafurucho.order.CreateOrderRequest;
import com.robertafurucho.order.OrderController;
import com.robertafurucho.order.OrderResponse;
import com.robertafurucho.order.OrderService;
import com.robertafurucho.order.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * <p>Uses {@code standaloneSetup} with the filter explicitly applied so the
 * filter is tested in isolation — no Spring context, no token bleed between
 * test classes. A fresh filter is created per test via {@code @BeforeEach},
 * guaranteeing clean bucket state.
 *
 * @decision Standalone MockMvc instead of @WebMvcTest because the filter is
 *           intentionally NOT a @Component (see WebConfig). This lets us
 *           test the filter in complete isolation.
 */
// Spring MockMvc builder methods use @NonNull params that trigger false positives
@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter")
class RateLimitingFilterTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(new ObjectMapper());
        mockMvc = MockMvcBuilders
            .standaloneSetup(new OrderController(orderService))
            .addFilters(filter)
            .build();
    }

    @AfterEach
    void tearDown() {
        filter.destroy();
    }

    /** Stub service to return a minimal valid OrderResponse. */
    private void stubServiceOk() {
        var response = new OrderResponse(
            1L, "Test", "t@t.com", "11999990000",
            "Rua X", "01234567", "Boneca", "Detalhe",
            FUTURE_DATE, LocalDateTime.now(), OrderStatus.PENDING
        );
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);
    }

    private String validBody() {
        return """
            {
                "name": "Test User",
                "email": "test@example.com",
                "phone": "11999990000",
                "address": "Rua X, 1",
                "postalCode": "01234-567",
                "orderScope": "Boneca de biscuit",
                "orderScopeDetail": "Descricao valida",
                "receiveDate": "%s"
            }
            """.formatted(FUTURE_DATE.toString());
    }

    @Test
    @DisplayName("first 5 POST requests are allowed (201)")
    void firstFiveRequests_AreAllowed() throws Exception {
        stubServiceOk();

        for (int i = 0; i < RateLimitingFilter.CAPACITY; i++) {
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("sixth POST request returns 429 with Retry-After header")
    void sixthRequest_ReturnsTooManyRequests() throws Exception {
        stubServiceOk();
        String body = validBody();

        for (int i = 0; i < RateLimitingFilter.CAPACITY; i++) {
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("Muitas requisições"))
            .andExpect(header().string("Retry-After",
                String.valueOf(RateLimitingFilter.RETRY_AFTER_SECONDS)));
    }

    @Test
    @DisplayName("GET requests are never rate-limited (no 429 after many calls)")
    void getRequests_AreNotRateLimited() throws Exception {
        when(orderService.getAllOrders()).thenReturn(java.util.List.of());

        for (int i = 0; i < RateLimitingFilter.CAPACITY + 3; i++) {
            mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("X-Forwarded-For header separates buckets per IP")
    void xForwardedFor_SeparatesBucketsPerIp() throws Exception {
        stubServiceOk();
        String body = validBody();

        // Exhaust the bucket for IP 10.0.0.1
        for (int i = 0; i < RateLimitingFilter.CAPACITY; i++) {
            mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "10.0.0.1")
                    .content(body))
                .andExpect(status().isCreated());
        }

        // Same IP → 429
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "10.0.0.1")
                .content(body))
            .andExpect(status().isTooManyRequests());

        // Different IP → still OK (fresh bucket)
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "10.0.0.2")
                .content(body))
            .andExpect(status().isCreated());
    }
}
