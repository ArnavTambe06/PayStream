package com.paystream.api.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class RazorGateProvider implements PaymentGateway {

    private final Random random = new Random();

    @Override
    public GatewayResponse charge(String idempotencyKey,
                                  BigDecimal amount, String currency) {
        log.info("[RazorGate] Attempting charge: {} {} key={}",
                amount, currency, idempotencyKey);

        // Simulate network latency
        simulateLatency();

        // Simulate 70% success, 30% failure
        // In real life this would be an HTTP call to Razorpay
        boolean success = random.nextInt(100) < 70;

        if (!success) {
            log.warn("[RazorGate] Charge failed for key={}", idempotencyKey);
            throw new RuntimeException("RazorGate: payment declined");
        }

        String refId = "RZP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[RazorGate] Charge successful: refId={}", refId);

        return GatewayResponse.builder()
                .success(true)
                .providerRefId(refId)
                .build();
    }

    @Override
    public String getProviderName() { return "RAZORGATE"; }

    private void simulateLatency() {
        try {
            Thread.sleep(100 + new Random().nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}