package com.paystream.api.controller;

import com.paystream.api.dto.response.ApiResponse;
import com.paystream.api.dto.response.WalletResponse;
import com.paystream.api.entity.AccountType;
import com.paystream.api.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<WalletResponse> wallets = walletService.getMyWallets(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallets fetched", wallets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam AccountType accountType) {
        WalletResponse wallet = walletService.createWallet(
                userDetails.getUsername(), accountType);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created", wallet));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID walletId) {
        WalletResponse wallet = walletService.getWalletById(
                walletId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched", wallet));
    }
}