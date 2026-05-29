package com.paystream.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DepositRequest {

    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit is ₹1")
    @DecimalMax(value = "100000.00", message = "Maximum deposit is ₹1,00,000")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    @Size(max = 100)
    private String idempotencyKey;
}