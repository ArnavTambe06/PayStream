package com.paystream.api.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class StripeGateProvider implements PaymentGateway {

    private final Random random = new Random();

    @Override
    public GatewayResponse charge(String idempotencyKey,
                                  BigDecimal amount, String currency) {
        log.info("[StripeGate] Fallback charge: {} {} key={}",
                amount, currency, idempotencyKey);

        simulateLatency();

        // Fallback is more reliable — 90% success
        boolean success = random.nextInt(100) < 90;

        if (!success) {
            log.warn("[StripeGate] Fallback also failed for key={}", idempotencyKey);
            return GatewayResponse.builder()
                    .success(false)
                    .failureReason("StripeGate: payment declined")
                    .build();
        }

        String refId = "STR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[StripeGate] Fallback charge successful: refId={}", refId);

        return GatewayResponse.builder()
                .success(true)
                .providerRefId(refId)
                .build();
    }

    @Override
    public String getProviderName() { return "STRIPEGATE"; }

    private void simulateLatency() {
        try {
            Thread.sleep(150 + new Random().nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}