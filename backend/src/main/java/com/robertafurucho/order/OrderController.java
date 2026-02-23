package com.robertafurucho.order;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for order management.
 * 
 * Provides endpoints for creating and retrieving doll orders.
 * Includes rate limiting to prevent abuse.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    
    // Rate limiting: Map of IP -> Bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order.
     * 
     * @param request The order creation request
     * @param httpRequest HTTP request for rate limiting
     * @return Created order or rate limit error
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        // Rate limiting check
        Bucket bucket = resolveBucket(getClientIp(httpRequest));
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "error", "Muitas requisições",
                    "message", "Por favor, aguarde antes de enviar outro pedido."
                ));
        }

        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets all orders (admin endpoint).
     * 
     * @return List of all orders
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Gets an order by ID.
     * 
     * @param id The order ID
     * @return The order or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Updates an order's status.
     * 
     * @param id The order ID
     * @param status The new status
     * @return Updated order
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /**
     * Creates or retrieves a rate limit bucket for the given key.
     * Limit: 5 requests per minute per IP.
     */
    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    /**
     * Extracts client IP from request, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
