package io.github.samzhu.docmcp.security;

import io.github.samzhu.docmcp.domain.model.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * API Key 認證過濾器
 * <p>
 * 從請求的 Authorization Header 或 X-API-Key Header 中提取 API Key，
 * 並進行認證驗證。
 * </p>
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // 嘗試從請求中提取 API Key
        Optional<String> apiKeyOpt = extractApiKey(request);

        if (apiKeyOpt.isPresent()) {
            String rawKey = apiKeyOpt.get();

            // 驗證 API Key
            Optional<ApiKey> validatedKey = apiKeyService.validateKey(rawKey);

            if (validatedKey.isPresent()) {
                ApiKey apiKey = validatedKey.get();

                // 更新最後使用時間（非同步）
                try {
                    apiKeyService.updateLastUsed(apiKey);
                } catch (Exception e) {
                    log.warn("Failed to update last used time for API key: {}", apiKey.getKeyPrefix(), e);
                }

                // 建立認證 Token
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_API_KEY"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        new ApiKeyPrincipal(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix()),
                        null,
                        authorities
                );

                // 設定到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated with API key: {}", apiKey.getKeyPrefix());
            } else {
                log.debug("Invalid API key provided");
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 從請求中提取 API Key
     */
    private Optional<String> extractApiKey(HttpServletRequest request) {
        // 優先從 Authorization Header 提取（Bearer token 格式）
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }

        // 其次從 X-API-Key Header 提取
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return Optional.of(apiKeyHeader);
        }

        return Optional.empty();
    }

    /**
     * API Key 認證主體
     *
     * @param id        金鑰 ID（TSID 格式）
     * @param name      金鑰名稱
     * @param keyPrefix 金鑰前綴
     */
    public record ApiKeyPrincipal(
            String id,
            String name,
            String keyPrefix
    ) {}
}
