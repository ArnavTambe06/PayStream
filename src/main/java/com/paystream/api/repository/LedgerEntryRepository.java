package com.paystream.api.repository;

import com.paystream.api.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("SELECT l FROM LedgerEntry l WHERE l.wallet.id = :walletId ORDER BY l.createdAt DESC")
    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);
}