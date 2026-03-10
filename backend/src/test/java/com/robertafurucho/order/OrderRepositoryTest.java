package com.robertafurucho.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link OrderRepository} custom queries.
 *
 * <p>Uses {@code @DataJpaTest} which auto-configures an embedded H2 database,
 * scans for {@code @Entity} classes, and rolls back after each test.
 */
@DataJpaTest
@DisplayName("OrderRepository")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(6);

    /**
     * Creates and persists a minimal valid Order with the given email and status.
     */
    private Order persistOrder(String email, OrderStatus status) {
        Order order = new Order();
        order.setName("Test User");
        order.setEmail(email);
        order.setPhone("11999990000");
        order.setAddress("Rua X, 1");
        order.setPostalCode("01234567");
        order.setOrderScope("Boneca");
        order.setOrderScopeDetail("Detalhe");
        order.setReceiveDate(FUTURE_DATE);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("findByEmailOrderByCreatedAtDesc")
    class FindByEmail {

        @Test
        @DisplayName("returns orders matching the email, newest first")
        void returnsOrdersForEmail() {
            persistOrder("alice@test.com", OrderStatus.PENDING);
            persistOrder("alice@test.com", OrderStatus.CONFIRMED);
            persistOrder("bob@test.com", OrderStatus.PENDING);

            List<Order> result = orderRepository.findByEmailOrderByCreatedAtDesc("alice@test.com");

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(o -> "alice@test.com".equals(o.getEmail()));
        }

        @Test
        @DisplayName("returns empty list when no orders match the email")
        void returnsEmptyWhenNoMatch() {
            persistOrder("alice@test.com", OrderStatus.PENDING);

            List<Order> result = orderRepository.findByEmailOrderByCreatedAtDesc("nobody@test.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatusOrderByCreatedAtDesc")
    class FindByStatus {

        @Test
        @DisplayName("returns orders matching the status")
        void returnsOrdersForStatus() {
            persistOrder("a@test.com", OrderStatus.PENDING);
            persistOrder("b@test.com", OrderStatus.CONFIRMED);
            persistOrder("c@test.com", OrderStatus.PENDING);

            List<Order> result = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(o -> OrderStatus.PENDING.equals(o.getStatus()));
        }

        @Test
        @DisplayName("returns empty list when no orders have the status")
        void returnsEmptyWhenNoMatch() {
            persistOrder("a@test.com", OrderStatus.PENDING);

            List<Order> result = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.SHIPPED);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreatedAtDesc")
    class FindAll {

        @Test
        @DisplayName("returns all orders")
        void returnsAllOrders() {
            persistOrder("a@test.com", OrderStatus.PENDING);
            persistOrder("b@test.com", OrderStatus.CONFIRMED);
            persistOrder("c@test.com", OrderStatus.SHIPPED);

            List<Order> result = orderRepository.findAllByOrderByCreatedAtDesc();

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("returns empty list when no orders exist")
        void returnsEmptyWhenNone() {
            List<Order> result = orderRepository.findAllByOrderByCreatedAtDesc();

            assertThat(result).isEmpty();
        }
    }
}
