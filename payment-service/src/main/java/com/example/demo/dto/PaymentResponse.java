package com.example.demo.dto;

import com.example.demo.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Payment response containing status and details")
public class PaymentResponse {
    @Schema(description = "Payment unique identifier", example = "pay_123456789")
    private String id;
    
    @Schema(description = "Current payment status", example = "PENDING")
    private PaymentStatus status;
    
    @Schema(description = "Response message", example = "Payment request received successfully")
    private String message;
    
    @Schema(description = "Response timestamp", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public PaymentResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentResponse(String id, PaymentStatus status, String message) {
        this();
        this.id = id;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

