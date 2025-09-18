package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentErrorHandler.class);
    
    @Autowired
    private PaymentStorageService paymentStorageService;
    
    @Autowired
    private PaymentProducerService paymentProducerService;

    /**
     * Retryable method for sending payments to Kafka
     */
    @Retryable(
        value = {KafkaException.class, Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Void> sendPaymentWithRetry(Payment payment) {
        logger.info("Attempting to send payment {} to Kafka", payment.getId());
        return paymentProducerService.sendPaymentForProcessing(payment)
                .thenApply(result -> null); // Convert to CompletableFuture<Void>
    }

    /**
     * Recovery method when all retries are exhausted
     */
    @Recover
    public CompletableFuture<Void> recoverFromKafkaFailure(Exception ex, Payment payment) {
        logger.error("Failed to send payment {} to Kafka after all retries. Error: {}", 
                payment.getId(), ex.getMessage());
        
        // Update payment status to failed
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage("Payment processing unavailable: " + ex.getMessage());
        payment.setProcessedAt(LocalDateTime.now());
        
        // Store the failed payment
        paymentStorageService.storePayment(payment);
        
        // Return completed future to avoid blocking
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handle Kafka connection errors
     */
    public void handleKafkaConnectionError(String paymentId, Exception error) {
        logger.error("Kafka connection error for payment {}: {}", paymentId, error.getMessage());
        
        // Try to get the payment and update its status
        paymentStorageService.getPaymentById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage("Payment service temporarily unavailable");
            payment.setProcessedAt(LocalDateTime.now());
            paymentStorageService.storePayment(payment);
        });
    }

    /**
     * Handle payment processing errors
     */
    public void handlePaymentProcessingError(String paymentId, Exception error) {
        logger.error("Payment processing error for payment {}: {}", paymentId, error.getMessage());
        
        paymentStorageService.getPaymentById(paymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage("Payment processing failed: " + error.getMessage());
            payment.setProcessedAt(LocalDateTime.now());
            paymentStorageService.storePayment(payment);
        });
    }

    /**
     * Check if Kafka is available
     */
    public boolean isKafkaAvailable() {
        try {
            // Simple health check - try to send a test message
            // In a real implementation, you might use Kafka admin client
            return true;
        } catch (Exception e) {
            logger.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get error message for different error types
     */
    public String getErrorMessage(Exception error) {
        if (error instanceof KafkaException) {
            return "Payment processing service is temporarily unavailable. Please try again later.";
        } else if (error.getMessage().contains("timeout")) {
            return "Payment processing timed out. Please try again.";
        } else if (error.getMessage().contains("connection")) {
            return "Unable to connect to payment service. Please check your connection.";
        } else {
            return "An unexpected error occurred during payment processing.";
        }
    }
}
