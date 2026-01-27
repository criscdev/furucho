package com.robertafurucho.order;

/**
 * Possible statuses for a doll order.
 */
public enum OrderStatus {
    /** Order received, awaiting confirmation */
    PENDING,
    
    /** Order confirmed, work in progress */
    CONFIRMED,
    
    /** Doll creation in progress */
    IN_PROGRESS,
    
    /** Order completed, ready for shipping */
    COMPLETED,
    
    /** Order shipped to customer */
    SHIPPED,
    
    /** Order cancelled */
    CANCELLED
}
