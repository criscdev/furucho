package com.robertafurucho.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that enforces per-IP rate limiting on POST /api/orders.
 *
 * <p>Limit: 5 requests per minute per IP address.
 * Requests that exceed the limit receive HTTP 429 with a JSON error body.
 * All other routes and methods pass through without restriction.
 *
 * @decision Extracted from OrderController to satisfy SRP — HTTP concern
 *           (rate limiting) must not live in the business controller.
 *           Registered via FilterRegistrationBean (not @Component) so that
 *           {@code @WebMvcTest} slices do NOT load it automatically, keeping
 *           controller unit tests isolated from token-consumption side effects.
 * @see WebConfig#rateLimitingFilter()
 */
public class RateLimitingFilter implements Filter {

    static final int CAPACITY = 5;
    static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        if (isRateLimited(httpReq)) {
            String clientIp = extractClientIp(httpReq);
            Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

            if (!bucket.tryConsume(1)) {
                httpResp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResp.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResp.getWriter().write(
                    "{\"error\":\"Muitas requisições\"," +
                    "\"message\":\"Por favor, aguarde antes de enviar outro pedido.\"}"
                );
                return;
            }
        }

        chain.doFilter(req, resp);
    }

    /**
     * Returns {@code true} only for {@code POST /api/orders} requests.
     *
     * @param req the incoming HTTP request
     * @return whether this request should be rate-limited
     */
    private boolean isRateLimited(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod())
            && req.getRequestURI().matches(".*/api/orders/?$");
    }

    /**
     * Extracts the real client IP, respecting {@code X-Forwarded-For} proxies.
     *
     * @param request the incoming HTTP request
     * @return the most-specific client IP address
     */
    String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Creates a new bucket with the configured capacity and refill policy.
     *
     * @return a fresh {@link Bucket} with a greedy 5-req/min limit
     */
    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, REFILL_PERIOD)
                .build())
            .build();
    }

    /**
     * Clears all IP buckets. Package-private for test isolation.
     *
     * @decision Exposed to allow {@code @BeforeEach} resets in tests without
     *           contaminating tests that share a Spring context.
     */
    void resetBuckets() {
        buckets.clear();
    }
}
