package com.robertafurucho.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Service layer for order business logic.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Creates a new order from a request DTO.
     * 
     * @param request The validated order creation request
     * @return The created order response
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setName(request.name());
        order.setEmail(request.email());
        order.setPhone(normalizePhone(request.phone()));
        order.setAddress(request.address());
        order.setPostalCode(normalizeCep(request.postalCode()));
        order.setOrderScope(request.orderScope());
        order.setOrderScopeDetail(request.orderScopeDetail());
        order.setReceiveDate(request.receiveDate());
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    /**
     * Gets all orders, newest first.
     * 
     * @return List of all orders
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderResponse::fromEntity)
            .toList();
    }

    /**
     * Gets an order by ID.
     * 
     * @param id The order ID
     * @return The order response
     * @throws OrderNotFoundException if order not found
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Objects.requireNonNull(id, "Order ID must not be null");
        return orderRepository.findById(id)
            .map(OrderResponse::fromEntity)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Gets orders by customer email.
     * 
     * @param email The customer email
     * @return List of orders for the email
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByEmail(String email) {
        return orderRepository.findByEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(OrderResponse::fromEntity)
            .toList();
    }

    /**
     * Updates an order's status.
     * 
     * @param id The order ID
     * @param status The new status
     * @return The updated order response
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Objects.requireNonNull(id, "Order ID must not be null");
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    /**
     * Normalizes phone number by removing non-digits.
     */
    private String normalizePhone(String phone) {
        return phone.replaceAll("\\D", "");
    }

    /**
     * Normalizes CEP by removing all non-digit characters.
     */
    private String normalizeCep(String cep) {
        return cep.replaceAll("\\D", "");
    }
}
