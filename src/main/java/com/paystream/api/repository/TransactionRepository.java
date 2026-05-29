package com.paystream.api.repository;

import com.paystream.api.entity.Transaction;
import com.paystream.api.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.fromWallet.id = :walletId OR t.toWallet.id = :walletId)
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.initiatedBy.id = :userId
        AND t.createdAt BETWEEN :from AND :to
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findByUserIdAndDateRange(
            UUID userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByFromWallet_IdAndCreatedAtAfter(UUID walletId, LocalDateTime after);
}