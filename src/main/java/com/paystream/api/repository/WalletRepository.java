package com.paystream.api.repository;

import com.paystream.api.entity.AccountType;
import com.paystream.api.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByUserId(UUID userId);
    Optional<Wallet> findByUserIdAndAccountType(UUID userId, AccountType accountType);
    boolean existsByUserIdAndAccountType(UUID userId, AccountType accountType);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.isActive = true")
    List<Wallet> findActiveWalletsByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(UUID id);
}