package com.robertafurucho.order;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for order responses.
 * 
 * Excludes sensitive data and formats dates appropriately.
 */
public record OrderResponse(
    Long id,
    String name,
    String email,
    String phone,
    String address,
    String postalCode,
    String orderScope,
    String orderScopeDetail,
    LocalDate receiveDate,
    LocalDateTime createdAt,
    OrderStatus status
) {
    /**
     * Creates an OrderResponse from an Order entity.
     * 
     * @param order The order entity to convert
     * @return OrderResponse DTO
     */
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getName(),
            order.getEmail(),
            order.getPhone(),
            order.getAddress(),
            order.getPostalCode(),
            order.getOrderScope(),
            order.getOrderScopeDetail(),
            order.getReceiveDate(),
            order.getCreatedAt(),
            order.getStatus()
        );
    }
}
