package com.paystream.api.service;

import com.paystream.api.dto.response.*;
import com.paystream.api.entity.*;
import com.paystream.api.exception.PayStreamException;
import com.paystream.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository        userRepository;
    private final WalletRepository      walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository    auditLogRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final CacheService          cacheService;

    // ─── DASHBOARD STATS ─────────────────────────────────────────────────

    @Cacheable(value = "adminStats", key = "'dashboard'")
    public DashboardStatsResponse getDashboardStats() {
        log.info("Cache MISS — computing admin dashboard stats");

        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalWallets(walletRepository.count())
                .totalTransactions(transactionRepository.count())
                .totalVolumeProcessed(
                        transactionRepository.sumSuccessfulTransactionAmounts())
                .successfulPayments(
                        paymentRequestRepository.countByStatus(PaymentStatus.SUCCESS))
                .failedPayments(
                        paymentRequestRepository.countByStatus(PaymentStatus.FAILED))
                .activeUsers(userRepository.countByIsActiveTrue())
                .build();
    }

    // ─── USER MANAGEMENT ─────────────────────────────────────────────────

    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toAdminUserResponse);
    }

    public AdminUserResponse getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toAdminUserResponse)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public AdminUserResponse deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));

        if (!user.isActive()) {
            throw new PayStreamException(
                    "User is already deactivated", HttpStatus.BAD_REQUEST);
        }

        user.setActive(false);
        userRepository.save(user);

        // Deactivate all their wallets too
        walletRepository.findByUserId(user.getId())
                .forEach(wallet -> {
                    wallet.setActive(false);
                    walletRepository.save(wallet);
                    cacheService.evictWalletCache(wallet.getId());
                });

        cacheService.evictUserProfileCache(user.getEmail());
        cacheService.evictAdminStatsCache();

        log.warn("Admin deactivated user: {}", user.getEmail());
        return toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse reactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));

        if (user.isActive()) {
            throw new PayStreamException(
                    "User is already active", HttpStatus.BAD_REQUEST);
        }

        user.setActive(true);
        userRepository.save(user);
        cacheService.evictAdminStatsCache();

        log.info("Admin reactivated user: {}", user.getEmail());
        return toAdminUserResponse(user);
    }

    // ─── TRANSACTION OVERSIGHT ────────────────────────────────────────────

    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(this::toTransactionResponse);
    }

    public Page<TransactionResponse> getTransactionsByUser(
            UUID userId, Pageable pageable) {
        return transactionRepository
                .findByUserIdAndDateRange(
                        userId,
                        java.time.LocalDateTime.now().minusYears(1),
                        java.time.LocalDateTime.now(),
                        pageable)
                .map(this::toTransactionResponse);
    }

    // ─── AUDIT LOGS ──────────────────────────────────────────────────────

    public Page<AuditLogResponse> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::toAuditLogResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByUser(
            UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable)
                .map(this::toAuditLogResponse);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .walletCount((int) walletRepository.countByUserId(user.getId()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .referenceId(t.getReferenceId())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .fromWalletId(t.getFromWallet() != null
                        ? t.getFromWallet().getId() : null)
                .toWalletId(t.getToWallet() != null
                        ? t.getToWallet().getId() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userEmail(log.getUser() != null
                        ? log.getUser().getEmail() : "system")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}