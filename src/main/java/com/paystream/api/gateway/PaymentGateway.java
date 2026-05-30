package com.paystream.api.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    GatewayResponse charge(String idempotencyKey, BigDecimal amount, String currency);
    String getProviderName();
}