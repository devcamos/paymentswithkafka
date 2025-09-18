package com.example.demo.controller;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.service.PaymentProducerService;
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

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentProducerService paymentProducerService;
    
    @Autowired
    private PaymentStorageService paymentStorageService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;

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
            
            // Send payment to Kafka for processing
            CompletableFuture<Void> future = paymentProducerService
                    .sendPaymentForProcessing(payment)
                    .thenAccept(result -> {
                        logger.info("Payment {} queued for processing", payment.getId());
                        // Send initial event
                        paymentProducerService.sendPaymentEvent(payment, "PAYMENT_CREATED");
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to queue payment {} for processing", payment.getId(), throwable);
                        // Update payment status to failed if Kafka is down
                        payment.setStatus(PaymentStatus.FAILED);
                        payment.setErrorMessage("Payment processing unavailable: " + throwable.getMessage());
                        paymentStorageService.storePayment(payment);
                        return null;
                    });
            
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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is running");
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String paymentId) {
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
    }
    
    /**
     * Get all payments with optional pagination
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
        Optional<Payment> payment = paymentStorageService.getPaymentById(paymentId);
        if (payment.isPresent()) {
            return ResponseEntity.ok(payment.get());
        } else {
            return ResponseEntity.notFound().build();
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
     * Get payments by merchant ID
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Payment>> getPaymentsByMerchantId(@PathVariable String merchantId) {
        try {
            List<Payment> payments = paymentStorageService.getPaymentsByMerchantId(merchantId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payments for merchant {}", merchantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getPaymentStats() {
        try {
            long totalPayments = paymentStorageService.getTotalPaymentCount();
            long pendingPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.PENDING).size();
            long processingPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.PROCESSING).size();
            long completedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.COMPLETED).size();
            long failedPayments = paymentStorageService.getPaymentsByStatus(PaymentStatus.FAILED).size();
            
            return ResponseEntity.ok(Map.of(
                "totalPayments", totalPayments,
                "pendingPayments", pendingPayments,
                "processingPayments", processingPayments,
                "completedPayments", completedPayments,
                "failedPayments", failedPayments
            ));
        } catch (Exception e) {
            logger.error("Error fetching payment statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}