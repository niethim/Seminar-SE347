package com.demo.voucher.service;

import com.demo.voucher.exception.RateLimitExceededException;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_SECOND = 100;
    private final Map<String, UserRateWindow> clientWindows = new ConcurrentHashMap<>();

    @Lock(LockType.READ)
    public void checkRateLimit(String key) {
        long currentSecond = System.currentTimeMillis() / 1000;
        UserRateWindow window = clientWindows.compute(key, (k, existing) -> {
            if (existing == null || existing.second != currentSecond) {
                return new UserRateWindow(currentSecond, 1);
            }
            existing.count++;
            return existing;
        });

        if (window.count > MAX_REQUESTS_PER_SECOND) {
            throw new RateLimitExceededException("Rate limit exceeded for client: " + key + ". Please try again later.");
        }
    }

    private static class UserRateWindow {
        final long second;
        int count;

        UserRateWindow(long second, int count) {
            this.second = second;
            this.count = count;
        }
    }
}
