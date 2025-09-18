package com.example.demo.service;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PaymentConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumerService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PaymentProducerService paymentProducerService;
    
    @Autowired
    private PaymentStorageService paymentStorageService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    private final Random random = new Random();

    @KafkaListener(topics = "${kafka.topic.payments}", groupId = "${spring.kafka.consumer.group-id}")
    public void processPayment(@Payload String paymentJson, 
                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            Payment payment = objectMapper.readValue(paymentJson, Payment.class);
            logger.info("Processing payment {} from topic {}, partition {}, offset {}", 
                    payment.getId(), topic, partition, offset);
            
            // Store payment in memory
            paymentStorageService.storePayment(payment);
            
            // Simulate payment processing
            processPaymentAsync(payment);
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing payment from topic {}", topic, e);
        }
    }

    private void processPaymentAsync(Payment payment) {
        // Update status to processing
        payment.setStatus(PaymentStatus.PROCESSING);
        sendWebSocketUpdate(payment);
        
        // Simulate async processing with random delay
        new Thread(() -> {
            try {
                // Simulate processing time (1-5 seconds)
                Thread.sleep(1000 + random.nextInt(4000));
                
                // Simulate success/failure (90% success rate)
                if (random.nextDouble() < 0.9) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setProcessedAt(LocalDateTime.now());
                    logger.info("Payment {} completed successfully", payment.getId());
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setErrorMessage("Payment processing failed due to insufficient funds");
                    payment.setProcessedAt(LocalDateTime.now());
                    logger.warn("Payment {} failed", payment.getId());
                }
                
                // Send final status update
                sendWebSocketUpdate(payment);
                
                // Send payment event
                paymentProducerService.sendPaymentEvent(payment, "PAYMENT_PROCESSED");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Payment processing interrupted for payment {}", payment.getId());
            }
        }).start();
    }

    private void sendWebSocketUpdate(Payment payment) {
        webSocketNotificationService.sendPaymentStatusUpdate(payment);
    }
}

