package com.paystream.api.controller;

import com.paystream.api.dto.response.*;
import com.paystream.api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // ← entire controller locked to ADMIN

public class AdminController {

    private final AdminService adminService;

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard stats",
                adminService.getDashboardStats()));
    }

    // ── Users ─────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Users fetched",
                adminService.getAllUsers(pageable)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "User fetched",
                adminService.getUserById(userId)));
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "User deactivated",
                adminService.deactivateUser(userId)));
    }

    @PatchMapping("/users/{userId}/reactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> reactivateUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "User reactivated",
                adminService.reactivateUser(userId)));
    }

    // ── Transactions ──────────────────────────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Transactions fetched",
                adminService.getAllTransactions(pageable)));
    }


    @GetMapping("/transactions/user/{userId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getUserTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("User transactions fetched",
                adminService.getTransactionsByUser(userId, pageable)));
    }

    // ── Audit Logs ────────────────────────────────────────────────────────


    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched",
                adminService.getAllAuditLogs(pageable)));
    }

    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("User audit logs fetched",
                adminService.getAuditLogsByUser(userId, pageable)));
    }
}