package com.paystream.api.repository;

import com.paystream.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Eagerly fetch user to avoid LazyInitializationException
    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.user ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllWithUser(Pageable pageable);

    // For user-specific audit logs
    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.user " +
            "WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}