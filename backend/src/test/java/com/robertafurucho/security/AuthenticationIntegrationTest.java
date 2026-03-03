package com.robertafurucho.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_WithValidCredentials_ReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "admin",
                        "password": "admin123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "admin",
                        "password": "wrongpassword"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void getAllOrders_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_WithValidToken_ReturnsOrders() throws Exception {
        // First, login to get token
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "username": "admin",
                        "password": "admin123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String token = response.substring(response.indexOf(":\"") + 2, response.lastIndexOf("\""));

        // Then, use token to access protected endpoint
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void getOrderById_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatus_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/orders/1/status")
                .param("status", "COMPLETED"))
            .andExpect(status().isForbidden());
    }
}
