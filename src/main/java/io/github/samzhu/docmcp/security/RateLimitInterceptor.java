package io.github.samzhu.docmcp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiting 攔截器
 * <p>
 * 使用記憶體計數實現簡單的 Rate Limiting。
 * 每小時重置計數器。
 * </p>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final int DEFAULT_RATE_LIMIT = 1000;
    private static final long HOUR_IN_MILLIS = 3600_000L;

    private final ApiKeyService apiKeyService;
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {
        // 取得認證資訊
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;  // 未認證的請求由 Security Config 處理
        }

        // 取得 API Key 資訊
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof ApiKeyAuthenticationFilter.ApiKeyPrincipal apiKeyPrincipal)) {
            return true;  // 非 API Key 認證
        }

        // 取得 API Key 的速率限制
        int rateLimit = DEFAULT_RATE_LIMIT;
        var apiKeyOpt = apiKeyService.getKeyById(apiKeyPrincipal.id());
        if (apiKeyOpt.isPresent()) {
            Integer customLimit = apiKeyOpt.get().rateLimit();
            if (customLimit != null && customLimit > 0) {
                rateLimit = customLimit;
            }
        }

        // 檢查速率限制
        String bucketKey = apiKeyPrincipal.keyPrefix();
        final int finalRateLimit = rateLimit;
        RateLimitBucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> new RateLimitBucket(finalRateLimit));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for API key: {}", apiKeyPrincipal.keyPrefix());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));

            response.getWriter().write("""
                    {"error": "Rate limit exceeded", "message": "Too many requests. Please try again later."}
                    """);

            return false;
        }

        // 添加 Rate Limit Headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));

        return true;
    }

    /**
     * 速率限制桶
     */
    private static class RateLimitBucket {
        private final int limit;
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        RateLimitBucket(int limit) {
            this.limit = limit;
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            long windowStartTime = windowStart.get();

            // 檢查是否需要重置窗口
            if (now - windowStartTime >= HOUR_IN_MILLIS) {
                // 重置計數器
                if (windowStart.compareAndSet(windowStartTime, now)) {
                    count.set(0);
                }
            }

            // 嘗試增加計數
            int current = count.incrementAndGet();
            return current <= limit;
        }

        int getRemaining() {
            return Math.max(0, limit - count.get());
        }

        long getResetTime() {
            return windowStart.get() + HOUR_IN_MILLIS;
        }
    }
}
