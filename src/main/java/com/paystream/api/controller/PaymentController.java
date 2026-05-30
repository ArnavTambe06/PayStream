package com.paystream.api.controller;

import com.paystream.api.dto.request.PaymentInitiateRequest;
import com.paystream.api.dto.response.ApiResponse;
import com.paystream.api.dto.response.PaymentResponse;
import com.paystream.api.service.PaymentOrchestratorService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrchestratorService orchestratorService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentInitiateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment initiated",
                orchestratorService.initiatePayment(
                        userDetails.getUsername(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched",
                orchestratorService.getMyPayments(
                        userDetails.getUsername(), pageable)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched",
                orchestratorService.getPaymentById(
                        userDetails.getUsername(), paymentId)));
    }
}