package com.example.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment processing status")
public enum PaymentStatus {
    @Schema(description = "Payment has been created and is awaiting processing")
    PENDING,
    
    @Schema(description = "Payment is currently being processed")
    PROCESSING,
    
    @Schema(description = "Payment has been successfully completed")
    COMPLETED,
    
    @Schema(description = "Payment processing has failed")
    FAILED,
    
    @Schema(description = "Payment has been cancelled")
    CANCELLED
}
