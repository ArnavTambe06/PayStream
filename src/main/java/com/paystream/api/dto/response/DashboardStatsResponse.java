package com.paystream.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalWallets;
    private long totalTransactions;
    private BigDecimal totalVolumeProcessed;
    private long successfulPayments;
    private long failedPayments;
    private long activeUsers;
}