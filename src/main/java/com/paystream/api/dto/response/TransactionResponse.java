package com.paystream.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class TransactionResponse {
    private UUID id;
    private String referenceId;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private UUID fromWalletId;
    private UUID toWalletId;
    private LocalDateTime createdAt;
}