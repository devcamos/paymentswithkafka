package com.example.demo.integration;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentStorageService paymentStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        paymentStorageService.clearAllPayments();
    }

    @Test
    void completePaymentFlow_ShouldWorkEndToEnd() throws Exception {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setUserId("integration-user-123");
        request.setMerchantId("integration-merchant-456");
        request.setAmount(new BigDecimal("150.75"));
        request.setCurrency("USD");
        request.setDescription("Integration test payment");

        // When & Then - Create payment
        String createResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Payment request received and queued for processing"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract payment ID from response
        String paymentId = objectMapper.readTree(createResponse).get("id").asText();

        // Simulate payment processing by manually updating status
        Payment payment = new Payment(
                request.getUserId(),
                request.getMerchantId(),
                request.getAmount(),
                request.getCurrency(),
                request.getDescription()
        );
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentStorageService.storePayment(payment);

        // When & Then - Get payment status
        mockMvc.perform(get("/api/payments/status/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // When & Then - Get payment details
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.userId").value("integration-user-123"))
                .andExpect(jsonPath("$.amount").value(150.75));

        // When & Then - Get payments by user
        mockMvc.perform(get("/api/payments/user/integration-user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value("integration-user-123"));

        // When & Then - Get all payments
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(paymentId));
    }

    @Test
    void paymentValidation_ShouldReturnProperError() throws Exception {
        // Given - Invalid payment request
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setUserId(""); // Empty user ID
        invalidRequest.setAmount(new BigDecimal("-50.0")); // Negative amount
        invalidRequest.setCurrency("INVALID"); // Invalid currency

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNonExistentPayment_ShouldReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/non-existent-payment-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentStats_ShouldReturnCorrectStatistics() throws Exception {
        // Given - Create test payments with different statuses
        Payment completedPayment = createTestPayment("payment-1", PaymentStatus.COMPLETED);
        Payment pendingPayment = createTestPayment("payment-2", PaymentStatus.PENDING);
        Payment failedPayment = createTestPayment("payment-3", PaymentStatus.FAILED);

        paymentStorageService.storePayment(completedPayment);
        paymentStorageService.storePayment(pendingPayment);
        paymentStorageService.storePayment(failedPayment);

        // When & Then
        mockMvc.perform(get("/api/payments/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(3))
                .andExpect(jsonPath("$.pendingPayments").value(1))
                .andExpect(jsonPath("$.completedPayments").value(1))
                .andExpect(jsonPath("$.failedPayments").value(1));
    }

    @Test
    void healthCheck_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment service is running"));
    }

    private Payment createTestPayment(String id, PaymentStatus status) {
        Payment payment = new Payment("test-user", "test-merchant", new BigDecimal("100.0"), "USD", "Test payment");
        payment.setId(id);
        payment.setStatus(status);
        return payment;
    }

    private String loadExpectedResponse(String filename) throws IOException {
        ClassPathResource resource = new ClassPathResource("test-responses/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
