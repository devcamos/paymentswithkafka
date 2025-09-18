package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentResilienceService;
import com.example.demo.service.PaymentStorageService;
import com.example.demo.service.WebSocketNotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Production-ready Payment Controller with Resilience Patterns
 * Implements strategies used by real payment companies
 */
@RestController
@RequestMapping("/api/payments/resilience")
@CrossOrigin(origins = "*")
public class PaymentResilienceController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentResilienceController.class);
    
    @Autowired
    private PaymentResilienceService paymentResilienceService;
    
    @Autowired
    private PaymentStorageService paymentStorageService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

    /**
     * Create payment with multiple fallback strategies
     * This is how companies like Dojo handle payment creation
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            logger.info("Received payment request for user {} to merchant {}", 
                    paymentRequest.getUserId(), paymentRequest.getMerchantId());
            
            // Create payment object
            Payment payment = new Payment(
                    paymentRequest.getUserId(),
                    paymentRequest.getMerchantId(),
                    paymentRequest.getAmount(),
                    paymentRequest.getCurrency(),
                    paymentRequest.getDescription()
            );
            
            // Send immediate WebSocket notification for payment creation
            webSocketNotificationService.sendPaymentCreatedNotification(payment);
            
            // Process with resilience patterns
            CompletableFuture<Payment> future = paymentResilienceService.processPaymentWithResilience(payment);
            
            // Return immediate response (non-blocking)
            PaymentResponse response = new PaymentResponse(
                    payment.getId(),
                    PaymentStatus.PENDING,
                    "Payment request received and queued for processing"
            );
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error creating payment", e);
            PaymentResponse errorResponse = new PaymentResponse(
                    null,
                    PaymentStatus.FAILED,
                    "Failed to create payment: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get payment status with fallback to database
     */
    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String paymentId) {
        try {
            Optional<Payment> payment = paymentStorageService.getPaymentById(paymentId);
            if (payment.isPresent()) {
                PaymentResponse response = new PaymentResponse(
                        payment.get().getId(),
                        payment.get().getStatus(),
                        "Payment status retrieved successfully"
                );
                return ResponseEntity.ok(response);
            } else {
                PaymentResponse response = new PaymentResponse(
                        paymentId,
                        PaymentStatus.FAILED,
                        "Payment not found"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("Error getting payment status for {}", paymentId, e);
            PaymentResponse response = new PaymentResponse(
                    paymentId,
                    PaymentStatus.FAILED,
                    "Unable to retrieve payment status"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get system health and resilience status
     * Critical for monitoring in production
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        try {
            PaymentResilienceService.SystemHealth health = paymentResilienceService.getSystemHealth();
            
            Map<String, Object> healthData = Map.of(
                "status", health.kafkaHealthy ? "HEALTHY" : "DEGRADED",
                "kafkaHealthy", health.kafkaHealthy,
                "kafkaAvailable", health.kafkaAvailable,
                "consecutiveFailures", health.consecutiveFailures,
                "queuedPayments", health.queuedPayments,
                "timestamp", System.currentTimeMillis()
            );
            
            HttpStatus status = health.kafkaHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(healthData);
            
        } catch (Exception e) {
            logger.error("Error getting detailed health", e);
            Map<String, Object> errorData = Map.of(
                "status", "ERROR",
                "message", "Unable to retrieve system health",
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorData);
        }
    }

    /**
     * Get all payments with pagination
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Payment> payments = paymentStorageService.getPayments(page, size);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        try {
            Optional<Payment> payment = paymentStorageService.getPaymentById(paymentId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(payment.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting payment {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get payments by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable String userId) {
        try {
            List<Payment> payments = paymentStorageService.getPaymentsByUserId(userId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payments for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get payments by status
     */
    @GetMapping("/status-filter/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        try {
            List<Payment> payments = paymentStorageService.getPaymentsByStatus(status);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payments with status {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get payment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        try {
            long totalPayments = paymentStorageService.getTotalPaymentCount();
            long pendingPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.PENDING).size();
            long processingPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.PROCESSING).size();
            long completedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED).size();
            long failedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.FAILED).size();
            
            Map<String, Object> stats = Map.of(
                "totalPayments", totalPayments,
                "pendingPayments", pendingPayments,
                "processingPayments", processingPayments,
                "completedPayments", completedPayments,
                "failedPayments", failedPayments,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching payment statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Simple health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is running");
    }
}
