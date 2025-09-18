package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentProducerService;
import com.example.demo.service.PaymentStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
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
class PaymentControllerTest {

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
    void createPayment_Success() throws Exception {
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

        verify(paymentProducerService).sendPaymentForProcessing(any(Payment.class));
    }

    @Test
    void createPayment_ValidationError() throws Exception {
        // Given
        PaymentRequest invalidRequest = new PaymentRequest();
        invalidRequest.setUserId(""); // Invalid: empty user ID
        invalidRequest.setAmount(new BigDecimal("-10.0")); // Invalid: negative amount

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_ServiceError() throws Exception {
        // Given
        when(paymentProducerService.sendPaymentForProcessing(any(Payment.class)))
                .thenThrow(new RuntimeException("Test error message"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPaymentRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to create payment: Test error message"));
    }

    @Test
    void getPaymentStatus_Success() throws Exception {
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
    void getPaymentStatus_NotFound() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("non-existent-id"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/status/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.id").value("non-existent-id"))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Payment not found"));
    }

    @Test
    void getAllPayments_Success() throws Exception {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentStorageService.getPayments(0, 10)).thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("test-payment-id-123"))
                .andExpect(jsonPath("$[0].userId").value("test-user-123"))
                .andExpect(jsonPath("$[0].merchantId").value("test-merchant-456"));
    }

    @Test
    void getAllPayments_WithPagination() throws Exception {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentStorageService.getPayments(1, 5)).thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPaymentById_Success() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("test-payment-id-123"))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/test-payment-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-payment-id-123"))
                .andExpect(jsonPath("$.userId").value("test-user-123"))
                .andExpect(jsonPath("$.amount").value(100.50));
    }

    @Test
    void getPaymentById_NotFound() throws Exception {
        // Given
        when(paymentStorageService.getPaymentById("non-existent-id"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByUserId_Success() throws Exception {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByUserId("test-user-123"))
                .thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments/user/test-user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value("test-user-123"));
    }

    @Test
    void getPaymentsByStatus_Success() throws Exception {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED))
                .thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments/status-filter/COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getPaymentsByMerchantId_Success() throws Exception {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentStorageService.getPaymentsByMerchantId("test-merchant-456"))
                .thenReturn(payments);

        // When & Then
        mockMvc.perform(get("/api/payments/merchant/test-merchant-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].merchantId").value("test-merchant-456"));
    }

    @Test
    void getPaymentStats_Success() throws Exception {
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
    void healthCheck_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment service is running"));
    }
}
