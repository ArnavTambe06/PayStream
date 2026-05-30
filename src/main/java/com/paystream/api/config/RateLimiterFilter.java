package com.paystream.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    // 60 requests per minute per user
    private static final int  MAX_REQUESTS = 60;
    private static final long WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Skip rate limiting for public endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        // Identify user by JWT email or IP as fallback
        String principal = extractPrincipal(request);
        String redisKey  = "rate_limit:" + principal;

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count == 1) {
            // First request in window — set expiry
            redisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        // Set rate limit headers (like GitHub API does)
        response.setHeader("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(
                Math.max(0, MAX_REQUESTS - count)));

        if (count > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for principal: {}", principal);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "success": false,
                  "message": "Too many requests. Limit: %d per minute.",
                  "timestamp": "%s"
                }
                """.formatted(MAX_REQUESTS, java.time.LocalDateTime.now()));
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractPrincipal(HttpServletRequest request) {
        // If JWT authenticated, use email — else fall back to IP
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        String ip = request.getHeader("X-Forwarded-For");
        return ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}