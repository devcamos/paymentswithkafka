package com.example.demo.service;

import com.example.demo.model.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending WebSocket notifications for payment updates
 * Centralizes WebSocket messaging logic
 */
@Service
public class WebSocketNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Send payment update to user-specific topic
     */
    public void sendUserPaymentUpdate(Payment payment) {
        try {
            String paymentJson = objectMapper.writeValueAsString(payment);
            messagingTemplate.convertAndSend("/topic/payments/" + payment.getUserId(), paymentJson);
            logger.debug("Sent WebSocket update for payment {} to user {}", 
                    payment.getId(), payment.getUserId());
        } catch (JsonProcessingException e) {
            logger.error("Error sending user WebSocket update for payment {}", payment.getId(), e);
        }
    }
    
    /**
     * Send payment update to global topic (all connected clients)
     */
    public void sendGlobalPaymentUpdate(Payment payment) {
        try {
            String paymentJson = objectMapper.writeValueAsString(payment);
            messagingTemplate.convertAndSend("/topic/payments/all", paymentJson);
            logger.debug("Sent global WebSocket update for payment {}", payment.getId());
        } catch (JsonProcessingException e) {
            logger.error("Error sending global WebSocket update for payment {}", payment.getId(), e);
        }
    }
    
    /**
     * Send payment update to both user-specific and global topics
     */
    public void sendPaymentUpdate(Payment payment) {
        sendUserPaymentUpdate(payment);
        sendGlobalPaymentUpdate(payment);
    }
    
    /**
     * Send payment created notification immediately
     */
    public void sendPaymentCreatedNotification(Payment payment) {
        logger.info("Sending payment created notification for payment {}", payment.getId());
        sendPaymentUpdate(payment);
    }
    
    /**
     * Send payment status update notification
     */
    public void sendPaymentStatusUpdate(Payment payment) {
        logger.info("Sending payment status update for payment {} with status {}", 
                payment.getId(), payment.getStatus());
        sendPaymentUpdate(payment);
    }
}
