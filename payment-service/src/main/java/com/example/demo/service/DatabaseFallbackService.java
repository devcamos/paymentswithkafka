package com.example.demo.service;

import com.example.demo.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;

/**
 * Database Fallback Service
 * Handles payments when Kafka is unavailable
 * Similar to Stripe's backup processing systems
 */
@Service
public class DatabaseFallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFallbackService.class);
    
    // In-memory storage for demo (in production, this would be a real database)
    private final Map<String, Payment> processingQueue = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Payment> manualReviewQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * Store payment for immediate database processing
     */
    public void storePaymentForProcessing(Payment payment) {
        logger.info("Storing payment {} for database processing", payment.getId());
        processingQueue.put(payment.getId(), payment);
    }
    
    /**
     * Store payment for manual review (when all systems fail)
     */
    public void storeForManualReview(Payment payment, Exception error) {
        logger.warn("Storing payment {} for manual review due to error: {}", 
                payment.getId(), error.getMessage());
        manualReviewQueue.offer(payment);
    }
    
    /**
     * Get payment from processing queue
     */
    public Payment getPaymentFromQueue(String paymentId) {
        return processingQueue.get(paymentId);
    }
    
    /**
     * Remove payment from processing queue
     */
    public void removeFromQueue(String paymentId) {
        processingQueue.remove(paymentId);
    }
    
    /**
     * Get all payments in manual review queue
     */
    public ConcurrentLinkedQueue<Payment> getManualReviewQueue() {
        return manualReviewQueue;
    }
    
    /**
     * Get processing queue size
     */
    public int getProcessingQueueSize() {
        return processingQueue.size();
    }
    
    /**
     * Get manual review queue size
     */
    public int getManualReviewQueueSize() {
        return manualReviewQueue.size();
    }
}
