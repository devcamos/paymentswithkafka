package com.example.demo.service;

import com.example.demo.model.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProducerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${kafka.topic.payments}")
    private String paymentsTopic;
    
    @Value("${kafka.topic.payment-events}")
    private String paymentEventsTopic;

    public CompletableFuture<SendResult<String, String>> sendPaymentForProcessing(Payment payment) {
        try {
            String paymentJson = objectMapper.writeValueAsString(payment);
            logger.info("Sending payment {} for processing", payment.getId());
            
            return kafkaTemplate.send(paymentsTopic, payment.getId(), paymentJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Payment {} sent successfully to topic {}", 
                                    payment.getId(), paymentsTopic);
                        } else {
                            logger.error("Failed to send payment {} to topic {}", 
                                    payment.getId(), paymentsTopic, ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Error serializing payment {}", payment.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<SendResult<String, String>> sendPaymentEvent(Payment payment, String eventType) {
        try {
            PaymentEvent event = new PaymentEvent(payment.getId(), eventType, payment);
            String eventJson = objectMapper.writeValueAsString(event);
            logger.info("Sending payment event {} for payment {}", eventType, payment.getId());
            
            return kafkaTemplate.send(paymentEventsTopic, payment.getId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Payment event {} sent successfully for payment {}", 
                                    eventType, payment.getId());
                        } else {
                            logger.error("Failed to send payment event {} for payment {}", 
                                    eventType, payment.getId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("Error serializing payment event for payment {}", payment.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public static class PaymentEvent {
        private String paymentId;
        private String eventType;
        private Payment payment;
        private long timestamp;

        public PaymentEvent() {
            this.timestamp = System.currentTimeMillis();
        }

        public PaymentEvent(String paymentId, String eventType, Payment payment) {
            this();
            this.paymentId = paymentId;
            this.eventType = eventType;
            this.payment = payment;
        }

        // Getters and Setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
