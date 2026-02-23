package com.robertafurucho.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
@DisplayName("OrderService")
class OrderServiceTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private final ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

    /** Stubs save to assign an ID and return the entity. */
    private void stubSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = Objects.requireNonNull(inv.getArgument(0, Order.class));
            o.setId(1L);
            return o;
        });
    }

    private CreateOrderRequest validRequest() {
        return new CreateOrderRequest(
            "Roberta Furucho",
            "roberta@example.com",
            "11987654321",
            "Rua das Bonecas, 42",
            "01234-567",
            "Boneca de pano",
            "Boneca com vestido azul e cabelo cacheado",
            FUTURE_DATE
        );
    }

    private Order capturedOrder() {
        verify(orderRepository).save(orderCaptor.capture());
        return Objects.requireNonNull(orderCaptor.getValue());
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("maps all request fields to the entity")
        void mapsAllFields() {
            stubSave();
            var req = validRequest();

            orderService.createOrder(req);

            var saved = capturedOrder();
            assertThat(saved.getName()).isEqualTo(req.name());
            assertThat(saved.getEmail()).isEqualTo(req.email());
            assertThat(saved.getAddress()).isEqualTo(req.address());
            assertThat(saved.getOrderScope()).isEqualTo(req.orderScope());
            assertThat(saved.getOrderScopeDetail()).isEqualTo(req.orderScopeDetail());
            assertThat(saved.getReceiveDate()).isEqualTo(req.receiveDate());
        }

        @Test
        @DisplayName("sets initial status to PENDING")
        void setsStatusPending() {
            stubSave();

            orderService.createOrder(validRequest());

            assertThat(capturedOrder().getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("returns response with ID from saved entity")
        void returnsResponse() {
            stubSave();
            var req = validRequest();

            var response = orderService.createOrder(req);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo(req.name());
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("normalizes phone by stripping non-digit characters")
        void normalizesPhone() {
            stubSave();
            var req = new CreateOrderRequest(
                "Test", "t@t.com", "(11) 98765-4321",
                "Rua X", "01234567", "Escopo", "Detalhe", FUTURE_DATE
            );

            orderService.createOrder(req);

            assertThat(capturedOrder().getPhone()).isEqualTo("11987654321");
        }

        @Test
        @DisplayName("normalizes CEP by removing hyphen")
        void normalizesCep() {
            stubSave();

            orderService.createOrder(validRequest()); // postalCode = "01234-567"

            assertThat(capturedOrder().getPostalCode()).isEqualTo("01234567");
        }

        @Test
        @DisplayName("keeps CEP unchanged when no hyphen present")
        void keepsCepWithoutHyphen() {
            stubSave();
            var req = new CreateOrderRequest(
                "Test", "t@t.com", "11987654321",
                "Rua X", "01234567", "Escopo", "Detalhe", FUTURE_DATE
            );

            orderService.createOrder(req);

            assertThat(capturedOrder().getPostalCode()).isEqualTo("01234567");
        }
    }
}
