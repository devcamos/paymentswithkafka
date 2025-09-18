package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStorageServiceTest {

    private PaymentStorageService paymentStorageService;
    private Payment testPayment1;
    private Payment testPayment2;
    private Payment testPayment3;

    @BeforeEach
    void setUp() {
        paymentStorageService = new PaymentStorageService();
        
        testPayment1 = new Payment("user1", "merchant1", new BigDecimal("100.0"), "USD", "Payment 1");
        testPayment1.setId("payment-1");
        testPayment1.setStatus(PaymentStatus.COMPLETED);
        testPayment1.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        testPayment2 = new Payment("user1", "merchant2", new BigDecimal("200.0"), "EUR", "Payment 2");
        testPayment2.setId("payment-2");
        testPayment2.setStatus(PaymentStatus.PENDING);
        testPayment2.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        
        testPayment3 = new Payment("user2", "merchant1", new BigDecimal("300.0"), "GBP", "Payment 3");
        testPayment3.setId("payment-3");
        testPayment3.setStatus(PaymentStatus.FAILED);
        testPayment3.setCreatedAt(LocalDateTime.now().minusMinutes(15));
    }

    @Test
    void storePayment_ShouldStorePayment() {
        // When
        paymentStorageService.storePayment(testPayment1);
        
        // Then
        Optional<Payment> retrieved = paymentStorageService.getPaymentById("payment-1");
        assertTrue(retrieved.isPresent());
        assertEquals(testPayment1, retrieved.get());
    }

    @Test
    void getPaymentById_ExistingPayment_ShouldReturnPayment() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        
        // When
        Optional<Payment> result = paymentStorageService.getPaymentById("payment-1");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testPayment1, result.get());
    }

    @Test
    void getPaymentById_NonExistingPayment_ShouldReturnEmpty() {
        // When
        Optional<Payment> result = paymentStorageService.getPaymentById("non-existent");
        
        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getAllPayments_ShouldReturnAllPayments() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        paymentStorageService.storePayment(testPayment2);
        paymentStorageService.storePayment(testPayment3);
        
        // When
        List<Payment> result = paymentStorageService.getAllPayments();
        
        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains(testPayment1));
        assertTrue(result.contains(testPayment2));
        assertTrue(result.contains(testPayment3));
    }

    @Test
    void getPaymentsByUserId_ShouldReturnUserPayments() {
        // Given
        paymentStorageService.storePayment(testPayment1); // user1
        paymentStorageService.storePayment(testPayment2); // user1
        paymentStorageService.storePayment(testPayment3); // user2
        
        // When
        List<Payment> result = paymentStorageService.getPaymentsByUserId("user1");
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(testPayment1));
        assertTrue(result.contains(testPayment2));
        assertFalse(result.contains(testPayment3));
    }

    @Test
    void getPaymentsByStatus_ShouldReturnPaymentsWithStatus() {
        // Given
        paymentStorageService.storePayment(testPayment1); // COMPLETED
        paymentStorageService.storePayment(testPayment2); // PENDING
        paymentStorageService.storePayment(testPayment3); // FAILED
        
        // When
        List<Payment> completedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED);
        List<Payment> pendingPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.PENDING);
        List<Payment> failedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.FAILED);
        
        // Then
        assertEquals(1, completedPayments.size());
        assertTrue(completedPayments.contains(testPayment1));
        
        assertEquals(1, pendingPayments.size());
        assertTrue(pendingPayments.contains(testPayment2));
        
        assertEquals(1, failedPayments.size());
        assertTrue(failedPayments.contains(testPayment3));
    }

    @Test
    void getPaymentsByMerchantId_ShouldReturnMerchantPayments() {
        // Given
        paymentStorageService.storePayment(testPayment1); // merchant1
        paymentStorageService.storePayment(testPayment2); // merchant2
        paymentStorageService.storePayment(testPayment3); // merchant1
        
        // When
        List<Payment> merchant1Payments = paymentStorageService.getPaymentsByMerchantId("merchant1");
        List<Payment> merchant2Payments = paymentStorageService.getPaymentsByMerchantId("merchant2");
        
        // Then
        assertEquals(2, merchant1Payments.size());
        assertTrue(merchant1Payments.contains(testPayment1));
        assertTrue(merchant1Payments.contains(testPayment3));
        
        assertEquals(1, merchant2Payments.size());
        assertTrue(merchant2Payments.contains(testPayment2));
    }

    @Test
    void getPayments_WithPagination_ShouldReturnCorrectPage() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        paymentStorageService.storePayment(testPayment2);
        paymentStorageService.storePayment(testPayment3);
        
        // When
        List<Payment> firstPage = paymentStorageService.getPayments(0, 2);
        List<Payment> secondPage = paymentStorageService.getPayments(1, 2);
        
        // Then
        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
    }

    @Test
    void getPayments_PageOutOfRange_ShouldReturnEmptyList() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        
        // When
        List<Payment> result = paymentStorageService.getPayments(5, 10);
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getTotalPaymentCount_ShouldReturnCorrectCount() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        paymentStorageService.storePayment(testPayment2);
        paymentStorageService.storePayment(testPayment3);
        
        // When
        long count = paymentStorageService.getTotalPaymentCount();
        
        // Then
        assertEquals(3, count);
    }

    @Test
    void updatePaymentStatus_ShouldUpdateStatus() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        assertEquals(PaymentStatus.COMPLETED, testPayment1.getStatus());
        
        // When
        paymentStorageService.updatePaymentStatus("payment-1", PaymentStatus.FAILED);
        
        // Then
        Optional<Payment> updated = paymentStorageService.getPaymentById("payment-1");
        assertTrue(updated.isPresent());
        assertEquals(PaymentStatus.FAILED, updated.get().getStatus());
    }

    @Test
    void updatePaymentStatus_NonExistingPayment_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> {
            paymentStorageService.updatePaymentStatus("non-existent", PaymentStatus.FAILED);
        });
    }

    @Test
    void clearAllPayments_ShouldRemoveAllPayments() {
        // Given
        paymentStorageService.storePayment(testPayment1);
        paymentStorageService.storePayment(testPayment2);
        assertEquals(2, paymentStorageService.getTotalPaymentCount());
        
        // When
        paymentStorageService.clearAllPayments();
        
        // Then
        assertEquals(0, paymentStorageService.getTotalPaymentCount());
        assertTrue(paymentStorageService.getAllPayments().isEmpty());
    }
}
