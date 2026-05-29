package com.paystream.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {

    @NotNull(message = "Source wallet ID is required")
    private UUID fromWalletId;

    @NotNull(message = "Destination wallet ID is required")
    private UUID toWalletId;

    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum transfer is ₹1")
    @DecimalMax(value = "200000.00", message = "Maximum transfer is ₹2,00,000")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;

    @NotBlank(message = "Idempotency key is required for transfers")
    @Size(max = 100)
    private String idempotencyKey;
}