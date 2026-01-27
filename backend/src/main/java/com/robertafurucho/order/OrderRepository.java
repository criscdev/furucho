package com.robertafurucho.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Order entity persistence operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders by a customer's email.
     * 
     * @param email Customer email address
     * @return List of orders matching the email
     */
    List<Order> findByEmailOrderByCreatedAtDesc(String email);
    
    /**
     * Find all orders by status.
     * 
     * @param status Order status to filter by
     * @return List of orders with the given status
     */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    /**
     * Find all orders ordered by creation date (newest first).
     * 
     * @return All orders sorted by creation date descending
     */
    List<Order> findAllByOrderByCreatedAtDesc();
}
