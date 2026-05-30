package com.paystream.api.repository;

import com.paystream.api.entity.PaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {
    Optional<PaymentRequest> findByIdempotencyKey(String idempotencyKey);
    Page<PaymentRequest> findByWalletId(UUID walletId, Pageable pageable);
    Page<PaymentRequest> findByInitiatedById(UUID userId, Pageable pageable);
}