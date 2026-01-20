package io.github.samzhu.docmcp.security;

import io.github.samzhu.docmcp.config.FeatureFlags;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 併發限制攔截器
 * <p>
 * 限制每個 API Key 同時進行的請求數量，保護後端資源。
 * 使用 Semaphore 實現 per-API-key 的併發控制。
 * </p>
 */
@Component
public class ConcurrencyLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyLimitInterceptor.class);
    private static final String SEMAPHORE_ATTR = "concurrency.semaphore";

    private final FeatureFlags featureFlags;
    private final int defaultLimit;
    private final int anonymousLimit;
    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final Semaphore anonymousSemaphore;

    public ConcurrencyLimitInterceptor(
            FeatureFlags featureFlags,
            @Value("${docmcp.security.concurrency.default-limit:10}") int defaultLimit,
            @Value("${docmcp.security.concurrency.anonymous-limit:5}") int anonymousLimit) {
        this.featureFlags = featureFlags;
        this.defaultLimit = defaultLimit;
        this.anonymousLimit = anonymousLimit;
        this.anonymousSemaphore = new Semaphore(anonymousLimit);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {
        // 檢查功能開關是否啟用
        if (!featureFlags.isConcurrencyLimit()) {
            return true;  // 功能關閉，直接放行
        }

        Semaphore semaphore = getSemaphore(request);

        // 嘗試立即取得許可（不等待）
        if (!semaphore.tryAcquire()) {
            log.warn("Concurrency limit exceeded for path: {}", request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-Concurrency-Limit", String.valueOf(getLimit(request)));
            response.setHeader("X-Concurrency-Active", String.valueOf(getLimit(request) - semaphore.availablePermits()));

            response.getWriter().write("""
                    {"error": "Too many concurrent requests", "message": "Please wait for current requests to complete."}
                    """);

            return false;
        }

        // 儲存 semaphore 供 afterCompletion 使用
        request.setAttribute(SEMAPHORE_ATTR, semaphore);

        // 添加併發資訊 Headers
        response.setHeader("X-Concurrency-Limit", String.valueOf(getLimit(request)));
        response.setHeader("X-Concurrency-Remaining", String.valueOf(semaphore.availablePermits()));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // 釋放許可
        Semaphore semaphore = (Semaphore) request.getAttribute(SEMAPHORE_ATTR);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    /**
     * 取得對應的 Semaphore
     */
    private Semaphore getSemaphore(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return anonymousSemaphore;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ApiKeyAuthenticationFilter.ApiKeyPrincipal apiKeyPrincipal) {
            String key = apiKeyPrincipal.keyPrefix();
            return semaphores.computeIfAbsent(key, k -> new Semaphore(defaultLimit));
        }

        return anonymousSemaphore;
    }

    /**
     * 取得限制數
     */
    private int getLimit(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return anonymousLimit;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof ApiKeyAuthenticationFilter.ApiKeyPrincipal) {
            return defaultLimit;
        }

        return anonymousLimit;
    }
}
