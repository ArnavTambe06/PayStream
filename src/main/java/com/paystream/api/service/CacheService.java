package com.paystream.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    public void evictWalletCache(UUID walletId) {
        evict("wallets", walletId.toString());
    }

    public void evictUserProfileCache(String email) {
        evict("userProfile", email);
    }

    public void evictAdminStatsCache() {
        evict("adminStats", "dashboard");
    }

    private void evict(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache [{}] key={}", cacheName, key);
        }
    }
}