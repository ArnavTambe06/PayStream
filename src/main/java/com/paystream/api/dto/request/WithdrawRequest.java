package com.paystream.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WithdrawRequest {

    @NotNull(message = "Wallet ID is required")
    private UUID walletId;

    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum withdrawal is ₹1")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    @Size(max = 100)
    private String idempotencyKey;
}