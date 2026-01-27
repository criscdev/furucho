package com.robertafurucho.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller for monitoring and deployment validation.
 * 
 * Provides a simple endpoint to verify the application is running
 * and can be used by load balancers, monitoring systems, and CI/CD pipelines.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Basic health check endpoint.
     * 
     * @return Health status with timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "furucho-api"
        ));
    }

    /**
     * Readiness check - verifies the app is ready to receive traffic.
     * Can be extended to check database connectivity, etc.
     * 
     * @return Readiness status
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        // TODO: Add database connectivity check when needed
        return ResponseEntity.ok(Map.of(
            "status", "READY",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Liveness check - verifies the app is alive (not deadlocked).
     * 
     * @return Liveness status
     */
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> live() {
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", Instant.now().toString()
        ));
    }
}
