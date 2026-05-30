package com.paystream.api.gateway;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class GatewayResponse {
    private boolean success;
    private String providerRefId;
    private String failureReason;
}