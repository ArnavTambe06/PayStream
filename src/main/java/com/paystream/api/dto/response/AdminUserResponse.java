package com.paystream.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class AdminUserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private boolean isActive;
    private int walletCount;
    private LocalDateTime createdAt;
}