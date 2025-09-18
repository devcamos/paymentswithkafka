package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Payment entity representing a payment transaction")
public class Payment {
    @Schema(description = "Payment unique identifier", example = "pay_123456789")
    private String id;
    
    @Schema(description = "User identifier who initiated the payment", example = "user123")
    private String userId;
    
    @Schema(description = "Merchant identifier receiving the payment", example = "merchant456")
    private String merchantId;
    
    @Schema(description = "Payment amount", example = "99.99")
    private BigDecimal amount;
    
    @Schema(description = "Currency code", example = "USD")
    private String currency;
    
    @Schema(description = "Current payment status", example = "PENDING")
    private PaymentStatus status;
    
    @Schema(description = "Payment description", example = "Product purchase")
    private String description;
    
    @Schema(description = "Payment creation timestamp", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Payment processing completion timestamp", example = "2024-01-15 10:30:45")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;
    
    @Schema(description = "Error message if payment failed", example = "Insufficient funds")
    private String errorMessage;

    public Payment() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public Payment(String userId, String merchantId, BigDecimal amount, String currency, String description) {
        this();
        this.userId = userId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
