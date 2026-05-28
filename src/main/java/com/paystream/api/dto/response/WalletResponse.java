package com.paystream.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class WalletResponse {
    private UUID id;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private boolean isActive;
    private LocalDateTime createdAt;
}