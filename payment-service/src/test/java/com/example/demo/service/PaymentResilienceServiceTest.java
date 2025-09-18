package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentResilienceServiceTest {

    @Mock
    private PaymentProducerService paymentProducerService;

    @Mock
    private PaymentStorageService paymentStorageService;

    @Mock
    private DatabaseFallbackService databaseFallbackService;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    private PaymentResilienceService paymentResilienceService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment(
                "user123",
                "merchant456",
                new BigDecimal("99.99"),
                "USD",
                "Test Payment"
        );
        
        // Manually create the service with mocked dependencies
        paymentResilienceService = new PaymentResilienceService();
        paymentResilienceService.paymentProducerService = paymentProducerService;
        paymentResilienceService.paymentStorageService = paymentStorageService;
        paymentResilienceService.databaseFallbackService = databaseFallbackService;
        paymentResilienceService.circuitBreakerService = circuitBreakerService;
    }

    @Test
    void testProcessPaymentWithKafkaSuccess() {
        // Given
        when(circuitBreakerService.isKafkaAvailable()).thenReturn(true);
        when(paymentProducerService.sendPaymentForProcessing(any(Payment.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Payment> result = paymentResilienceService.processPaymentWithResilience(testPayment);

        // Then
        assertNotNull(result);
        // Wait for async processing to complete
        result.join();
        verify(paymentStorageService).storePayment(testPayment);
        verify(paymentProducerService).sendPaymentForProcessing(testPayment);
    }

    @Test
    void testProcessPaymentWithKafkaFailure() {
        // Given
        when(circuitBreakerService.isKafkaAvailable()).thenReturn(false);
        doNothing().when(databaseFallbackService).storePaymentForProcessing(any(Payment.class));

        // When
        CompletableFuture<Payment> result = paymentResilienceService.processPaymentWithResilience(testPayment);

        // Then
        assertNotNull(result);
        // Wait for async processing to complete
        result.join();
        verify(databaseFallbackService).storePaymentForProcessing(testPayment);
        verify(paymentProducerService, never()).sendPaymentForProcessing(any(Payment.class));
    }

    @Test
    void testProcessPaymentWithCompleteFailure() {
        // Given
        when(circuitBreakerService.isKafkaAvailable()).thenReturn(false);
        doThrow(new RuntimeException("Database unavailable"))
                .when(databaseFallbackService).storePaymentForProcessing(any(Payment.class));
        doNothing().when(databaseFallbackService).storeForManualReview(any(Payment.class), any(Exception.class));

        // When
        CompletableFuture<Payment> result = paymentResilienceService.processPaymentWithResilience(testPayment);

        // Then
        assertNotNull(result);
        // Wait for async processing to complete
        result.join();
        verify(databaseFallbackService).storeForManualReview(any(Payment.class), any(Exception.class));
    }

    @Test
    void testGetSystemHealth() {
        // Given
        when(circuitBreakerService.isKafkaAvailable()).thenReturn(true);

        // When
        PaymentResilienceService.SystemHealth health = paymentResilienceService.getSystemHealth();

        // Then
        assertNotNull(health);
        assertTrue(health.kafkaHealthy);
        assertTrue(health.kafkaAvailable);
    }

    @Test
    void testCircuitBreakerActivation() {
        // Given
        when(circuitBreakerService.isKafkaAvailable()).thenReturn(false);
        
        // Simulate circuit breaker activation by setting kafkaHealthy to false
        paymentResilienceService.kafkaHealthy = false;

        // When
        PaymentResilienceService.SystemHealth health = paymentResilienceService.getSystemHealth();

        // Then
        assertNotNull(health);
        assertFalse(health.kafkaHealthy);
        assertFalse(health.kafkaAvailable);
    }
}
