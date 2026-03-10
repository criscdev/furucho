package com.robertafurucho.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces per-IP rate limiting on POST requests.
 *
 * <p>Limit: 5 requests per minute per IP address.
 * Requests that exceed the limit receive HTTP 429 with a JSON error body
 * and a {@code Retry-After} header (RFC 6585).
 * Only POST requests are rate-limited; all other methods pass through.
 *
 * <p><strong>URL scoping:</strong> This filter is registered via
 * {@link WebConfig#rateLimitingFilter()} with URL patterns
 * {@code /api/orders} and {@code /api/orders/}. The servlet container
 * restricts which requests reach this filter — the filter itself only
 * checks the HTTP method.
 *
 * <p><strong>Eviction:</strong> Stale IP buckets are purged every
 * {@link #REFILL_PERIOD} to prevent unbounded memory growth.
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
    static final long RETRY_AFTER_SECONDS = REFILL_PERIOD.toSeconds();

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService evictionScheduler;

    /**
     * Creates a filter with the given {@link ObjectMapper} for JSON serialisation.
     *
     * @param objectMapper Jackson mapper (Spring-managed for consistent config)
     */
    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-eviction");
            t.setDaemon(true);
            return t;
        });
        evictionScheduler.scheduleAtFixedRate(
            () -> buckets.entrySet().removeIf(e ->
                    e.getValue().getAvailableTokens() >= CAPACITY),
            REFILL_PERIOD.toSeconds(),
            REFILL_PERIOD.toSeconds(),
            TimeUnit.SECONDS
        );
    }

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
                httpResp.setHeader("Retry-After", String.valueOf(RETRY_AFTER_SECONDS));
                objectMapper.writeValue(httpResp.getWriter(), Map.of(
                    "error", "Muitas requisições",
                    "message", "Por favor, aguarde antes de enviar outro pedido."
                ));
                return;
            }
        }

        chain.doFilter(req, resp);
    }

    /**
     * Returns {@code true} only for POST requests.
     *
     * <p>URL scoping is handled by the servlet container via
     * {@link WebConfig#rateLimitingFilter()} — this method only
     * checks the HTTP method to avoid duplicating URL patterns.
     *
     * @param req the incoming HTTP request
     * @return whether this request should be rate-limited
     */
    private boolean isRateLimited(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod());
    }

    /**
     * Extracts the real client IP, respecting {@code X-Forwarded-For} proxies.
     *
     * @param request the incoming HTTP request
     * @return the most-specific client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
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

    @Override
    public void destroy() {
        evictionScheduler.shutdownNow();
    }
}
