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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
            "Boneca de biscuit",
            "Boneca de biscuit com vestido azul e cabelo cacheado",
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

    // -------------------------------------------------------------------------
    // Batch 2B helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a persisted-like Order entity with an ID set.
     * {@code createdAt} is left null (no setter exists; set by @PrePersist in prod).
     *
     * @param id     entity ID
     * @param email  customer email
     * @param status order status
     * @return Order entity ready for stub responses
     */
    private Order buildOrder(Long id, String email, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setName("Test User");
        o.setEmail(email);
        o.setPhone("11999999999");
        o.setAddress("Rua X, 1");
        o.setPostalCode("01234567");
        o.setOrderScope("Boneca");
        o.setOrderScopeDetail("Vestido azul");
        o.setReceiveDate(FUTURE_DATE);
        o.setStatus(status);
        return o;
    }

    // -------------------------------------------------------------------------
    // Batch 2B — GetAllOrders
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllOrders")
    class GetAllOrders {

        @Test
        @DisplayName("returns empty list when repository is empty")
        void returnsEmptyList() {
            when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

            var result = orderService.getAllOrders();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns all orders mapped to responses")
        void returnsMappedOrders() {
            var orders = List.of(
                buildOrder(1L, "a@a.com", OrderStatus.PENDING),
                buildOrder(2L, "b@b.com", OrderStatus.CONFIRMED)
            );
            when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(orders);

            var result = orderService.getAllOrders();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(0).status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("preserves repository ordering (newest first)")
        void preservesOrdering() {
            var older = buildOrder(1L, "a@a.com", OrderStatus.PENDING);
            var newer = buildOrder(2L, "b@b.com", OrderStatus.PENDING);
            when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newer, older));

            var result = orderService.getAllOrders();

            assertThat(result.get(0).id()).isEqualTo(2L);
            assertThat(result.get(1).id()).isEqualTo(1L);
        }
    }

    // -------------------------------------------------------------------------
    // Batch 2B — GetOrderById
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("returns response for existing order")
        void returnsExistingOrder() {
            var order = buildOrder(42L, "c@c.com", OrderStatus.IN_PROGRESS);
            when(orderRepository.findById(42L)).thenReturn(Optional.of(order));

            var result = orderService.getOrderById(42L);

            assertThat(result.id()).isEqualTo(42L);
            assertThat(result.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("throws OrderNotFoundException when ID not found")
        void throwsWhenNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(99L)
            );
        }

        @Test
        @DisplayName("exception message includes the missing ID")
        void exceptionMessageContainsId() {
            when(orderRepository.findById(7L)).thenReturn(Optional.empty());

            var ex = org.junit.jupiter.api.Assertions.assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(7L)
            );
            assertThat(ex.getMessage()).contains("7");
        }
    }

    // -------------------------------------------------------------------------
    // Batch 2B — GetOrdersByEmail
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getOrdersByEmail")
    class GetOrdersByEmail {

        @Test
        @DisplayName("returns orders matching the email")
        void returnsMatchingOrders() {
            var email = "roberta@furucho.com";
            var orders = List.of(
                buildOrder(1L, email, OrderStatus.PENDING),
                buildOrder(2L, email, OrderStatus.CONFIRMED)
            );
            when(orderRepository.findByEmailOrderByCreatedAtDesc(email)).thenReturn(orders);

            var result = orderService.getOrdersByEmail(email);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(r -> r.email().equals(email));
        }

        @Test
        @DisplayName("returns empty list when no orders for that email")
        void returnsEmptyForUnknownEmail() {
            when(orderRepository.findByEmailOrderByCreatedAtDesc("ghost@x.com")).thenReturn(List.of());

            var result = orderService.getOrdersByEmail("ghost@x.com");

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Batch 2B — UpdateOrderStatus
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("updates status and returns new response")
        void updatesStatus() {
            var order = buildOrder(10L, "d@d.com", OrderStatus.PENDING);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            var result = orderService.updateOrderStatus(10L, OrderStatus.CONFIRMED);

            assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("persists the new status to the repository")
        void persistsNewStatus() {
            var order = buildOrder(11L, "e@e.com", OrderStatus.PENDING);
            when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0, Order.class));

            orderService.updateOrderStatus(11L, OrderStatus.IN_PROGRESS);

            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.IN_PROGRESS));
        }

        @Test
        @DisplayName("throws OrderNotFoundException when order not found")
        void throwsWhenNotFound() {
            when(orderRepository.findById(404L)).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(404L, OrderStatus.CONFIRMED)
            );
        }
    }
}
