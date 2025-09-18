package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Payment Resilience Service - Production-grade error handling
 * Inspired by real payment companies like Stripe, PayPal, Dojo
 */
@Service
public class PaymentResilienceService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentResilienceService.class);
    
    @Autowired
    PaymentProducerService paymentProducerService;
    
    @Autowired
    PaymentStorageService paymentStorageService;
    
    @Autowired
    DatabaseFallbackService databaseFallbackService;
    
    @Autowired
    CircuitBreakerService circuitBreakerService;
    
    // Local queue for failed payments (like Stripe's retry queue)
    private final ConcurrentLinkedQueue<Payment> failedPaymentsQueue = new ConcurrentLinkedQueue<>();
    
    // Scheduled executor for retry logic
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    
    // Circuit breaker state
    volatile boolean kafkaHealthy = true;
    private volatile long lastFailureTime = 0;
    private volatile int consecutiveFailures = 0;
    
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT = 60000; // 1 minute
    private static final long RETRY_DELAY = 5000; // 5 seconds

    /**
     * Primary payment processing with multiple fallback strategies
     * This is how companies like Dojo handle payment processing
     */
    public CompletableFuture<Payment> processPaymentWithResilience(Payment payment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Strategy 1: Try Kafka first (if circuit breaker allows)
                if (kafkaHealthy && circuitBreakerService.isKafkaAvailable()) {
                    return processWithKafka(payment);
                }
                
                // Strategy 2: Fallback to database processing
                logger.warn("Kafka unavailable, falling back to database processing for payment {}", payment.getId());
                return processWithDatabaseFallback(payment);
                
            } catch (Exception e) {
                logger.error("All processing strategies failed for payment {}", payment.getId(), e);
                return handleCompleteFailure(payment, e);
            }
        });
    }

    /**
     * Process payment through Kafka (primary path)
     */
    private Payment processWithKafka(Payment payment) {
        try {
            // Update status to processing
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentStorageService.storePayment(payment);
            
            // Send to Kafka
            paymentProducerService.sendPaymentForProcessing(payment)
                .thenAccept(result -> {
                    logger.info("Payment {} successfully queued in Kafka", payment.getId());
                    resetFailureCount();
                })
                .exceptionally(throwable -> {
                    logger.error("Kafka processing failed for payment {}", payment.getId(), throwable);
                    handleKafkaFailure(payment, throwable);
                    return null;
                });
            
            return payment;
            
        } catch (Exception e) {
            logger.error("Kafka processing error for payment {}", payment.getId(), e);
            handleKafkaFailure(payment, e);
            throw e;
        }
    }

    /**
     * Database fallback processing (like Stripe's backup systems)
     */
    private Payment processWithDatabaseFallback(Payment payment) {
        try {
            // Store in database for immediate processing
            payment.setStatus(PaymentStatus.PROCESSING);
            databaseFallbackService.storePaymentForProcessing(payment);
            
            // Process immediately in database
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulate payment processing
                    Thread.sleep(2000 + (int)(Math.random() * 3000));
                    
                    // 95% success rate for database processing
                    if (Math.random() < 0.95) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setProcessedAt(LocalDateTime.now());
                        logger.info("Payment {} completed via database fallback", payment.getId());
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setErrorMessage("Payment processing failed");
                        payment.setProcessedAt(LocalDateTime.now());
                        logger.warn("Payment {} failed via database fallback", payment.getId());
                    }
                    
                    paymentStorageService.storePayment(payment);
                    
                } catch (Exception e) {
                    logger.error("Database fallback processing failed for payment {}", payment.getId(), e);
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorMessage("Payment processing failed: " + e.getMessage());
                    paymentStorageService.storePayment(payment);
                }
            });
            
            return payment;
            
        } catch (Exception e) {
            logger.error("Database fallback failed for payment {}", payment.getId(), e);
            throw e;
        }
    }

    /**
     * Handle Kafka failures with circuit breaker pattern
     */
    private void handleKafkaFailure(Payment payment, Throwable error) {
        consecutiveFailures++;
        lastFailureTime = System.currentTimeMillis();
        
        // Activate circuit breaker if too many failures
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            kafkaHealthy = false;
            logger.error("Circuit breaker activated - Kafka marked as unhealthy");
            
            // Schedule circuit breaker reset
            retryExecutor.schedule(() -> {
                kafkaHealthy = true;
                consecutiveFailures = 0;
                logger.info("Circuit breaker reset - Kafka marked as healthy");
            }, CIRCUIT_BREAKER_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        
        // Add to retry queue for later processing
        failedPaymentsQueue.offer(payment);
        scheduleRetry();
    }

    /**
     * Schedule retry for failed payments (like Stripe's retry mechanism)
     */
    private void scheduleRetry() {
        retryExecutor.schedule(() -> {
            if (!failedPaymentsQueue.isEmpty() && kafkaHealthy) {
                Payment payment = failedPaymentsQueue.poll();
                if (payment != null) {
                    logger.info("Retrying payment {} after Kafka recovery", payment.getId());
                    processWithKafka(payment);
                }
            }
        }, RETRY_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Handle complete system failure
     */
    private Payment handleCompleteFailure(Payment payment, Exception error) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage("Payment service temporarily unavailable. Please try again later.");
        payment.setProcessedAt(LocalDateTime.now());
        paymentStorageService.storePayment(payment);
        
        // Store for manual review (like PayPal's manual review queue)
        databaseFallbackService.storeForManualReview(payment, error);
        
        return payment;
    }

    /**
     * Reset failure count on successful processing
     */
    private void resetFailureCount() {
        consecutiveFailures = 0;
    }

    /**
     * Get system health status
     */
    public SystemHealth getSystemHealth() {
        return new SystemHealth(
            kafkaHealthy,
            consecutiveFailures,
            failedPaymentsQueue.size(),
            circuitBreakerService.isKafkaAvailable()
        );
    }

    /**
     * System health data class
     */
    public static class SystemHealth {
        public final boolean kafkaHealthy;
        public final int consecutiveFailures;
        public final int queuedPayments;
        public final boolean kafkaAvailable;

        public SystemHealth(boolean kafkaHealthy, int consecutiveFailures, int queuedPayments, boolean kafkaAvailable) {
            this.kafkaHealthy = kafkaHealthy;
            this.consecutiveFailures = consecutiveFailures;
            this.queuedPayments = queuedPayments;
            this.kafkaAvailable = kafkaAvailable;
        }
    }
}
