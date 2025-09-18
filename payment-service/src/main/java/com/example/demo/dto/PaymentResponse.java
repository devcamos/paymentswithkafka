package com.example.demo.dto;

import com.example.demo.model.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class PaymentResponse {
    private String id;
    private PaymentStatus status;
    private String message;
    
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

