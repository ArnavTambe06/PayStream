package com.paystream.api.service;

import com.paystream.api.dto.request.DepositRequest;
import com.paystream.api.dto.request.TransferRequest;
import com.paystream.api.dto.request.WithdrawRequest;
import com.paystream.api.dto.response.TransactionResponse;
import com.paystream.api.entity.*;
import com.paystream.api.exception.PayStreamException;
import com.paystream.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    // Max single transaction amounts
    private static final BigDecimal MAX_DEPOSIT    = new BigDecimal("100000.00");
    private static final BigDecimal MAX_WITHDRAWAL = new BigDecimal("50000.00");
    private static final BigDecimal MAX_TRANSFER   = new BigDecimal("200000.00");

    // Velocity check: max transactions per hour per wallet
    private static final long MAX_TXN_PER_HOUR = 10;

    private final WalletRepository      walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditLogRepository    auditLogRepository;
    private final UserRepository        userRepository;

    // ─── DEPOSIT ────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse deposit(String email, DepositRequest request) {

        // 1. Idempotency — if same key exists, return existing result
        if (request.getIdempotencyKey() != null) {
            var existing = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate deposit request detected: {}", request.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }

        User user = getUserByEmail(email);

        // 2. Acquire pessimistic lock on wallet row
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));

        // 3. Ownership check
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }

        // 4. Validate wallet is active
        if (!wallet.isActive()) {
            throw new PayStreamException("Wallet is inactive", HttpStatus.BAD_REQUEST);
        }

        // 5. Fraud check — amount limit
        if (request.getAmount().compareTo(MAX_DEPOSIT) > 0) {
            throw new PayStreamException(
                    "Amount exceeds maximum deposit limit", HttpStatus.BAD_REQUEST);
        }

        // 6. Velocity check — max transactions per hour
        checkVelocity(wallet.getId());

        // 7. Record balance before change
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.add(request.getAmount());

        // 8. Update wallet balance
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // 9. Create master transaction record
        Transaction txn = Transaction.builder()
                .referenceId(generateReferenceId())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .description(request.getDescription())
                .toWallet(wallet)
                .initiatedBy(user)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        transactionRepository.save(txn);

        // 10. Double-entry: CREDIT on the wallet
        saveLedgerEntry(txn, wallet, EntryType.CREDIT,
                request.getAmount(), balanceBefore, balanceAfter);

        // 11. Audit log
        saveAuditLog(user, "DEPOSIT", "TRANSACTION", txn.getId().toString(),
                "Deposited ₹" + request.getAmount() + " to wallet " + wallet.getId());

        log.info("Deposit successful: {} → wallet {}", txn.getReferenceId(), wallet.getId());
        return toResponse(txn);
    }

    // ─── WITHDRAWAL ─────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse withdraw(String email, WithdrawRequest request) {

        if (request.getIdempotencyKey() != null) {
            var existing = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) return toResponse(existing.get());
        }

        User user = getUserByEmail(email);

        // Pessimistic lock
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));

        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }
        if (!wallet.isActive()) {
            throw new PayStreamException("Wallet is inactive", HttpStatus.BAD_REQUEST);
        }
        if (request.getAmount().compareTo(MAX_WITHDRAWAL) > 0) {
            throw new PayStreamException(
                    "Amount exceeds maximum withdrawal limit", HttpStatus.BAD_REQUEST);
        }

        // Insufficient funds check
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new PayStreamException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        checkVelocity(wallet.getId());

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.subtract(request.getAmount());

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        Transaction txn = Transaction.builder()
                .referenceId(generateReferenceId())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .description(request.getDescription())
                .fromWallet(wallet)
                .initiatedBy(user)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        transactionRepository.save(txn);

        // Double-entry: DEBIT on the wallet
        saveLedgerEntry(txn, wallet, EntryType.DEBIT,
                request.getAmount(), balanceBefore, balanceAfter);

        saveAuditLog(user, "WITHDRAWAL", "TRANSACTION", txn.getId().toString(),
                "Withdrew ₹" + request.getAmount() + " from wallet " + wallet.getId());

        return toResponse(txn);
    }

    // ─── TRANSFER ───────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse transfer(String email, TransferRequest request) {

        // Idempotency — critical for transfers (money movement)
        var existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) return toResponse(existing.get());

        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw new PayStreamException(
                    "Cannot transfer to the same wallet", HttpStatus.BAD_REQUEST);
        }

        User user = getUserByEmail(email);

        // Lock BOTH wallets in consistent order (lower UUID first)
        // This prevents deadlocks when two transfers happen in opposite directions
        UUID firstId  = request.getFromWalletId().compareTo(request.getToWalletId()) < 0
                ? request.getFromWalletId() : request.getToWalletId();
        UUID secondId = firstId.equals(request.getFromWalletId())
                ? request.getToWalletId() : request.getFromWalletId();

        Wallet first  = walletRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));
        Wallet second = walletRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));

        Wallet fromWallet = first.getId().equals(request.getFromWalletId()) ? first : second;
        Wallet toWallet   = first.getId().equals(request.getToWalletId())   ? first : second;

        // Validations
        if (!fromWallet.getUser().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied on source wallet", HttpStatus.FORBIDDEN);
        }
        if (!fromWallet.isActive() || !toWallet.isActive()) {
            throw new PayStreamException("One or both wallets are inactive", HttpStatus.BAD_REQUEST);
        }
        if (request.getAmount().compareTo(MAX_TRANSFER) > 0) {
            throw new PayStreamException(
                    "Amount exceeds maximum transfer limit", HttpStatus.BAD_REQUEST);
        }
        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new PayStreamException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        checkVelocity(fromWallet.getId());

        // Capture balances before
        BigDecimal fromBefore = fromWallet.getBalance();
        BigDecimal fromAfter  = fromBefore.subtract(request.getAmount());
        BigDecimal toBefore   = toWallet.getBalance();
        BigDecimal toAfter    = toBefore.add(request.getAmount());

        // Update both wallets atomically
        fromWallet.setBalance(fromAfter);
        toWallet.setBalance(toAfter);
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        // One transaction record for the TRANSFER_OUT side
        Transaction txn = Transaction.builder()
                .referenceId(generateReferenceId())
                .type(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .currency(fromWallet.getCurrency())
                .description(request.getDescription())
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .initiatedBy(user)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        transactionRepository.save(txn);

        // Double-entry: DEBIT from source, CREDIT to destination
        saveLedgerEntry(txn, fromWallet, EntryType.DEBIT,
                request.getAmount(), fromBefore, fromAfter);
        saveLedgerEntry(txn, toWallet, EntryType.CREDIT,
                request.getAmount(), toBefore, toAfter);

        saveAuditLog(user, "TRANSFER", "TRANSACTION", txn.getId().toString(),
                "Transferred ₹" + request.getAmount()
                        + " from " + fromWallet.getId()
                        + " to " + toWallet.getId());

        log.info("Transfer successful: {} ₹{} from {} to {}",
                txn.getReferenceId(), request.getAmount(),
                fromWallet.getId(), toWallet.getId());

        return toResponse(txn);
    }

    // ─── HISTORY ────────────────────────────────────────────────────────────

    public Page<TransactionResponse> getWalletHistory(
            String email, UUID walletId, Pageable pageable) {

        User user = getUserByEmail(email);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new PayStreamException(
                        "Wallet not found", HttpStatus.NOT_FOUND));

        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }

        return transactionRepository
                .findByWalletId(walletId, pageable)
                .map(this::toResponse);
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────

    private void checkVelocity(UUID walletId) {
        long recentCount = transactionRepository
                .countByFromWallet_IdAndCreatedAtAfter(
                        walletId, LocalDateTime.now().minusHours(1));
        if (recentCount >= MAX_TXN_PER_HOUR) {
            throw new PayStreamException(
                    "Transaction limit exceeded. Max " + MAX_TXN_PER_HOUR
                            + " transactions per hour.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private void saveLedgerEntry(Transaction txn, Wallet wallet,
                                 EntryType type, BigDecimal amount,
                                 BigDecimal before, BigDecimal after) {
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transaction(txn)
                .wallet(wallet)
                .entryType(type)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .build());
    }

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

    private String generateReferenceId() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new PayStreamException(
                        "User not found", HttpStatus.NOT_FOUND));
    }

    private TransactionResponse toResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .referenceId(txn.getReferenceId())
                .type(txn.getType().name())
                .status(txn.getStatus().name())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .description(txn.getDescription())
                .fromWalletId(txn.getFromWallet() != null
                        ? txn.getFromWallet().getId() : null)
                .toWalletId(txn.getToWallet() != null
                        ? txn.getToWallet().getId() : null)
                .createdAt(txn.getCreatedAt())
                .build();
    }
}