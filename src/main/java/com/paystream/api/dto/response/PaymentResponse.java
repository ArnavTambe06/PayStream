package com.paystream.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class PaymentResponse {
    private UUID id;
    private String provider;
    private String providerRefId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private int retryCount;
    private LocalDateTime createdAt;
}