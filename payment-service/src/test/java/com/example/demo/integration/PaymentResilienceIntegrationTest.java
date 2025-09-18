package com.example.demo.integration;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentResilienceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PaymentResilienceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private PaymentResilienceService paymentResilienceService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testCreatePaymentWithResilience() throws Exception {
        // Given
        PaymentRequest paymentRequest = new PaymentRequest(
                "user123",
                "merchant456",
                new BigDecimal("99.99"),
                "USD",
                "Resilience Test Payment"
        );

        Payment mockPayment = new Payment(
                "user123",
                "merchant456",
                new BigDecimal("99.99"),
                "USD",
                "Resilience Test Payment"
        );

        when(paymentResilienceService.processPaymentWithResilience(any(Payment.class)))
                .thenReturn(CompletableFuture.completedFuture(mockPayment));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Payment request received and queued for processing"));
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment service is running"));
    }

    @Test
    void testDetailedHealthCheck() throws Exception {
        // Given
        PaymentResilienceService.SystemHealth mockHealth = 
                new PaymentResilienceService.SystemHealth(true, 0, 0, true);

        when(paymentResilienceService.getSystemHealth()).thenReturn(mockHealth);

        // When & Then
        mockMvc.perform(get("/api/payments/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.kafkaHealthy").value(true))
                .andExpect(jsonPath("$.kafkaAvailable").value(true));
    }

    @Test
    void testDetailedHealthCheckDegraded() throws Exception {
        // Given
        PaymentResilienceService.SystemHealth mockHealth = 
                new PaymentResilienceService.SystemHealth(false, 5, 10, false);

        when(paymentResilienceService.getSystemHealth()).thenReturn(mockHealth);

        // When & Then
        mockMvc.perform(get("/api/payments/health/detailed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.kafkaHealthy").value(false))
                .andExpect(jsonPath("$.kafkaAvailable").value(false))
                .andExpect(jsonPath("$.consecutiveFailures").value(5))
                .andExpect(jsonPath("$.queuedPayments").value(10));
    }
}
