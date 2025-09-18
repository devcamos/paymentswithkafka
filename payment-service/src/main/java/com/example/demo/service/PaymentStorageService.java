package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PaymentStorageService {
    
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    
    /**
     * Store a payment in memory
     */
    public void storePayment(Payment payment) {
        payments.put(payment.getId(), payment);
    }
    
    /**
     * Get a payment by ID
     */
    public Optional<Payment> getPaymentById(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }
    
    /**
     * Get all payments
     */
    public List<Payment> getAllPayments() {
        return new ArrayList<>(payments.values());
    }
    
    /**
     * Get payments by user ID
     */
    public List<Payment> getPaymentsByUserId(String userId) {
        return payments.values().stream()
                .filter(payment -> payment.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get payments by status
     */
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return payments.values().stream()
                .filter(payment -> payment.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Get payments by merchant ID
     */
    public List<Payment> getPaymentsByMerchantId(String merchantId) {
        return payments.values().stream()
                .filter(payment -> payment.getMerchantId().equals(merchantId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get payments with pagination
     */
    public List<Payment> getPayments(int page, int size) {
        List<Payment> allPayments = new ArrayList<>(payments.values());
        allPayments.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt())); // Sort by creation date desc
        
        int start = page * size;
        int end = Math.min(start + size, allPayments.size());
        
        if (start >= allPayments.size()) {
            return new ArrayList<>();
        }
        
        return allPayments.subList(start, end);
    }
    
    /**
     * Get total count of payments
     */
    public long getTotalPaymentCount() {
        return payments.size();
    }
    
    /**
     * Update payment status
     */
    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = payments.get(paymentId);
        if (payment != null) {
            payment.setStatus(status);
        }
    }
    
    /**
     * Clear all payments (for testing)
     */
    public void clearAllPayments() {
        payments.clear();
    }
}
