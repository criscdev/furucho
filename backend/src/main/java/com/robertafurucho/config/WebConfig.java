package com.robertafurucho.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration: CORS settings and filter registration.
 */
@Configuration
public class WebConfig {

    /**
     * Registers the rate limiting filter for POST /api/orders only.
     *
     * @decision Uses FilterRegistrationBean instead of @Component so that
     *           {@code @WebMvcTest} slices do NOT auto-load the filter,
     *           keeping controller unit tests free of token-consumption
     *           side effects. See D2 in TDD_REFACTORING_PLAN.md.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RateLimitingFilter(objectMapper));
        bean.addUrlPatterns("/api/orders", "/api/orders/");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://robertafurucho.com",
                        "https://www.robertafurucho.com"
                    )
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
