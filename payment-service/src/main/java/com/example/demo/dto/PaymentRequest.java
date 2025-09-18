package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payment request payload for creating new payments")
public class PaymentRequest {
    @Schema(description = "Unique identifier of the user making the payment", example = "user123", required = true)
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Schema(description = "Unique identifier of the merchant receiving the payment", example = "merchant456", required = true)
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @Schema(description = "Payment amount", example = "99.99", required = true)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Schema(description = "Currency code (ISO 4217)", example = "USD", required = true)
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @Schema(description = "Optional payment description", example = "Product purchase - Premium subscription")
    private String description;

    // Constructors
    public PaymentRequest() {}

    public PaymentRequest(String userId, String merchantId, BigDecimal amount, String currency, String description) {
        this.userId = userId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
