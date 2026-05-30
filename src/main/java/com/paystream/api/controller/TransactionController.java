package com.paystream.api.controller;

import com.paystream.api.dto.request.DepositRequest;
import com.paystream.api.dto.request.TransferRequest;
import com.paystream.api.dto.request.WithdrawRequest;
import com.paystream.api.dto.response.ApiResponse;
import com.paystream.api.dto.response.TransactionResponse;
import com.paystream.api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Deposit successful",
                transactionService.deposit(userDetails.getUsername(), request)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful",
                transactionService.withdraw(userDetails.getUsername(), request)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Transfer successful",
                transactionService.transfer(userDetails.getUsername(), request)));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getWalletHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Transaction history fetched",
                transactionService.getWalletHistory(
                        userDetails.getUsername(), walletId, pageable)));
    }
}