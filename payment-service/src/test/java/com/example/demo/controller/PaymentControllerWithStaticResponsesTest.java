package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentProducerService;
import com.example.demo.service.PaymentStorageService;
import com.example.demo.util.TestResponseLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerWithStaticResponsesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentProducerService paymentProducerService;

    @MockBean
    private PaymentStorageService paymentStorageService;

    private Payment testPayment;
    private PaymentRequest testPaymentRequest;

    @BeforeEach
    void setUp() {
        testPayment = new Payment(
                "test-user-123",
                "test-merchant-456",
                new BigDecimal("100.50"),
                "USD",
                "Test payment"
        );
        testPayment.setId("test-payment-id-123");
        testPayment.setStatus(PaymentStatus.COMPLETED);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setProcessedAt(LocalDateTime.now().plusSeconds(5));

        testPaymentRequest = new PaymentRequest();
        testPaymentRequest.setUserId("test-user-123");
        testPaymentRequest.setMerchantId("test-merchant-456");
        testPaymentRequest.setAmount(new BigDecimal("100.50"));
        testPaymentRequest.setCurrency("USD");
        testPaymentRequest.setDescription("Test payment");
    }

    @Test
    void createPayment_Success_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentProducerService.sendPaymentForProcessing(any(Payment.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPaymentRequest)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Payment request received and queued for processing"));
    }

    @Test
    void createPayment_ServiceError_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentProducerService.sendPaymentForProcessing(any(Payment.class)))
                .thenThrow(new RuntimeException("Test error message"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPaymentRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.id").isEmpty())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to create payment: Test error message"));
    }

    @Test
    void getPaymentStatus_Success_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("test-payment-id-123"))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/status/test-payment-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-payment-id-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value("Payment status retrieved successfully"));
    }

    @Test
    void getPaymentStatus_NotFound_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("non-existent-payment-id"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/status/non-existent-payment-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.id").value("non-existent-payment-id"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Payment not found"));
    }

    @Test
    void getPaymentById_Success_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("test-payment-id-123"))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/test-payment-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-payment-id-123"))
                .andExpect(jsonPath("$.userId").value("test-user-123"))
                .andExpect(jsonPath("$.merchantId").value("test-merchant-456"))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.description").value("Test payment"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getAllPayments_Success_ShouldMatchExpectedResponse() throws Exception {
        // Given
        Payment payment2 = new Payment("test-user-456", "test-merchant-789", new BigDecimal("250.75"), "EUR", "Test payment 2");
        payment2.setId("test-payment-id-456");
        payment2.setStatus(PaymentStatus.PENDING);
        payment2.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        List<Payment> payments = Arrays.asList(testPayment, payment2);
        when(paymentStorageService.getPayments(0, 10)).thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("test-payment-id-123"))
                .andExpect(jsonPath("$[0].userId").value("test-user-123"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].id").value("test-payment-id-456"))
                .andExpect(jsonPath("$[1].userId").value("test-user-456"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void getPaymentsByUserId_Success_ShouldReturnUserPayments() throws Exception {
        // Given
        List<Payment> userPayments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByUserId("test-user-123"))
                .thenReturn(userPayments);

        // When & Then
        mockMvc.perform(get("/api/payments/user/test-user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value("test-user-123"))
                .andExpect(jsonPath("$[0].id").value("test-payment-id-123"));
    }

    @Test
    void getPaymentsByStatus_Success_ShouldReturnPaymentsWithStatus() throws Exception {
        // Given
        List<Payment> completedPayments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED))
                .thenReturn(completedPayments);

        // When & Then
        mockMvc.perform(get("/api/payments/status-filter/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].id").value("test-payment-id-123"));
    }

    @Test
    void getPaymentsByMerchantId_Success_ShouldReturnMerchantPayments() throws Exception {
        // Given
        List<Payment> merchantPayments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByMerchantId("test-merchant-456"))
                .thenReturn(merchantPayments);

        // When & Then
        mockMvc.perform(get("/api/payments/merchant/test-merchant-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].merchantId").value("test-merchant-456"))
                .andExpect(jsonPath("$[0].id").value("test-payment-id-123"));
    }

    @Test
    void getPaymentStats_Success_ShouldMatchExpectedResponse() throws Exception {
        // Given
        when(paymentStorageService.getTotalPaymentCount()).thenReturn(5L);
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.PENDING)).thenReturn(Arrays.asList(new Payment()));
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.PROCESSING)).thenReturn(Arrays.asList());
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED)).thenReturn(Arrays.asList(new Payment(), new Payment(), new Payment()));
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.FAILED)).thenReturn(Arrays.asList(new Payment()));

        // When & Then
        mockMvc.perform(get("/api/payments/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(5))
                .andExpect(jsonPath("$.pendingPayments").value(1))
                .andExpect(jsonPath("$.processingPayments").value(0))
                .andExpect(jsonPath("$.completedPayments").value(3))
                .andExpect(jsonPath("$.failedPayments").value(1));
    }

    @Test
    void healthCheck_Success_ShouldReturnExpectedMessage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment service is running"));
    }

    @Test
    void createPayment_ValidationError_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid payment request
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setUserId(""); // Empty user ID
        invalidRequest.setAmount(new BigDecimal("-10.0")); // Negative amount
        invalidRequest.setCurrency("INVALID"); // Invalid currency

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
