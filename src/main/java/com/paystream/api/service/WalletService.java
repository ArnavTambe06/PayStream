package com.paystream.api.service;

import com.paystream.api.dto.response.WalletResponse;
import com.paystream.api.entity.AccountType;
import com.paystream.api.entity.User;
import com.paystream.api.entity.Wallet;
import com.paystream.api.exception.PayStreamException;
import com.paystream.api.repository.UserRepository;
import com.paystream.api.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public List<WalletResponse> getMyWallets(String email) {
        User user = getUserByEmail(email);
        return walletRepository.findActiveWalletsByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WalletResponse createWallet(String email, AccountType accountType) {
        User user = getUserByEmail(email);

        if (walletRepository.existsByUserIdAndAccountType(user.getId(), accountType)) {
            throw new PayStreamException(
                    accountType.name() + " wallet already exists", HttpStatus.CONFLICT);
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .accountType(accountType)
                .currency("INR")
                .isActive(true)
                .build();

        return toResponse(walletRepository.save(wallet));
    }

    public WalletResponse getWalletById(UUID walletId, String email) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new PayStreamException("Wallet not found", HttpStatus.NOT_FOUND));

        // Ensure wallet belongs to the requesting user
        if (!wallet.getUser().getEmail().equals(email)) {
            throw new PayStreamException("Access denied", HttpStatus.FORBIDDEN);
        }

        return toResponse(wallet);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new PayStreamException("User not found", HttpStatus.NOT_FOUND));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .accountType(wallet.getAccountType().name())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .isActive(wallet.isActive())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}