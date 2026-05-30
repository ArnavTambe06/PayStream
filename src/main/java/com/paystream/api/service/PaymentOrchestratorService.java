package com.paystream.api.service;

import com.paystream.api.dto.request.PaymentInitiateRequest;
import com.paystream.api.dto.response.PaymentResponse;
import com.paystream.api.entity.*;
import com.paystream.api.exception.PayStreamException;
import com.paystream.api.gateway.GatewayResponse;
import com.paystream.api.gateway.RazorGateProvider;
import com.paystream.api.gateway.StripeGateProvider;
import com.paystream.api.repository.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestratorService {

    private final RazorGateProvider      razorGate;
    private final StripeGateProvider     stripeGate;
    private final WalletRepository       walletRepository;
    private final UserRepository         userRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final AuditLogRepository     auditLogRepository;
    private final TransactionService     transactionService;

    // ─── INITIATE PAYMENT ──────────────────────────────────────────────────

    @Transactional
    public PaymentResponse initiatePayment(String email,
                                           PaymentInitiateRequest request) {
        // 1. Idempotency check
        var existing = paymentRequestRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Duplicate payment request: {}", request.getIdempotencyKey());
            return toResponse(existing.get());
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));

        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }

        // 2. Build payment record in INITIATED state
        PaymentRequest payment = PaymentRequest.builder()
                .provider(PaymentProvider.RAZORGATE)
                .status(PaymentStatus.INITIATED)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .idempotencyKey(request.getIdempotencyKey())
                .wallet(wallet)
                .initiatedBy(user)
                .build();
        paymentRequestRepository.save(payment);

        // 3. Attempt charge via circuit-breaker-protected primary provider
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            GatewayResponse gatewayResponse = chargeWithCircuitBreaker(
                    request.getIdempotencyKey(),
                    request.getAmount(),
                    wallet.getCurrency()
            );

            payment.setProviderRefId(gatewayResponse.getProviderRefId());
            payment.setStatus(PaymentStatus.SUCCESS);

            // 4. On success, trigger a deposit into the wallet
            var depositRequest = new com.paystream.api.dto.request.DepositRequest();
            depositRequest.setWalletId(wallet.getId());
            depositRequest.setAmount(request.getAmount());
            depositRequest.setDescription("Payment via " + payment.getProvider().name());
            depositRequest.setIdempotencyKey("DEP-" + request.getIdempotencyKey());

            var txn = transactionService.deposit(email, depositRequest);
            payment.setTransaction(
                    new com.paystream.api.entity.Transaction());

            saveAuditLog(user, "PAYMENT_SUCCESS", "PAYMENT",
                    payment.getId().toString(),
                    "Payment successful via " + payment.getProvider()
                            + " ref=" + gatewayResponse.getProviderRefId());

        } catch (Exception ex) {
            log.error("Payment failed after all attempts: {}", ex.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());

            saveAuditLog(user, "PAYMENT_FAILED", "PAYMENT",
                    payment.getId().toString(),
                    "Payment failed: " + ex.getMessage());
        }

        payment = paymentRequestRepository.save(payment);
        payment = paymentRequestRepository.findById(payment.getId()).orElse(payment);
        return toResponse(payment);
    }

    // ─── CIRCUIT BREAKER + RETRY + FALLBACK ───────────────────────────────

    /**
     * Primary provider call wrapped in:
     * - @Retry: tries up to 3 times on IOException/TimeoutException
     * - @CircuitBreaker: if 50% of calls fail, opens the circuit
     *   and routes directly to fallbackCharge()
     */
    @Retry(name = "razorgate")
    @CircuitBreaker(name = "razorgate", fallbackMethod = "fallbackCharge")
    public GatewayResponse chargeWithCircuitBreaker(String idempotencyKey,
                                                    java.math.BigDecimal amount,
                                                    String currency) {
        log.info("[Orchestrator] Trying RazorGate for key={}", idempotencyKey);
        return razorGate.charge(idempotencyKey, amount, currency);
    }

    /**
     * Fallback: called automatically by Resilience4j when the circuit is OPEN
     * or when chargeWithCircuitBreaker() throws an exception after all retries.
     * Method signature must match + add Throwable parameter.
     */
    public GatewayResponse fallbackCharge(String idempotencyKey,
                                          java.math.BigDecimal amount,
                                          String currency,
                                          Throwable ex) {
        log.warn("[Orchestrator] Circuit open or retries exhausted. " +
                "Falling back to StripeGate. Reason: {}", ex.getMessage());
        return stripeGate.charge(idempotencyKey, amount, currency);
    }

    // ─── QUERY ────────────────────────────────────────────────────────────

    public Page<PaymentResponse> getMyPayments(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));
        return paymentRequestRepository
                .findByInitiatedById(user.getId(), pageable)
                .map(this::toResponse);
    }

    public PaymentResponse getPaymentById(String email, UUID paymentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));
        PaymentRequest payment = paymentRequestRepository.findById(paymentId)
                .orElseThrow(() -> new PayStreamException(
                        "Payment not found", HttpStatus.NOT_FOUND));
        if (!payment.getInitiatedBy().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }
        return toResponse(payment);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────

    private void saveAuditLog(User user, String action,
                              String entityType, String entityId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    private PaymentResponse toResponse(PaymentRequest p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .provider(p.getProvider().name())
                .providerRefId(p.getProviderRefId())
                .status(p.getStatus().name())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .failureReason(p.getFailureReason())
                .retryCount(p.getRetryCount())
                .createdAt(p.getCreatedAt())
                .build();
    }
}