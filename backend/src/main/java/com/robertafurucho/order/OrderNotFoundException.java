package com.robertafurucho.order;

/**
 * Exception thrown when an order is not found.
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(Long id) {
        super("Pedido não encontrado com ID: " + id);
    }
}
