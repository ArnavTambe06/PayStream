package com.paystream.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentInitiateRequest {

    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum payment is ₹1")
    @DecimalMax(value = "100000.00", message = "Maximum payment is ₹1,00,000")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    private String idempotencyKey;
}