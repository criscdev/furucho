package com.robertafurucho.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController.
 */
@WebMvcTest(
    controllers = HealthController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.robertafurucho.security.JwtAuthenticationFilter.class,
            com.robertafurucho.security.SecurityConfig.class,
            com.robertafurucho.security.JwtUtil.class,
            com.robertafurucho.security.AdminUserDetailsService.class
        }
    )
)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_ReturnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("furucho-api"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void ready_ReturnsReadyStatus() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void live_ReturnsAliveStatus() throws Exception {
        mockMvc.perform(get("/api/health/live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ALIVE"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
