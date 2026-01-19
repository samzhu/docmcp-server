package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.model.ApiKey;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Key 資料傳輸物件
 * <p>
 * 不包含金鑰雜湊等敏感資訊。
 * </p>
 *
 * @param id         金鑰 ID
 * @param name       識別名稱
 * @param keyPrefix  金鑰前綴
 * @param status     狀態
 * @param rateLimit  速率限制
 * @param expiresAt  過期時間
 * @param lastUsedAt 最後使用時間
 * @param createdAt  建立時間
 * @param createdBy  建立者
 */
public record ApiKeyDto(
        UUID id,
        String name,
        String keyPrefix,
        ApiKeyStatus status,
        Integer rateLimit,
        OffsetDateTime expiresAt,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt,
        String createdBy
) {
    /**
     * 從 ApiKey 實體轉換
     */
    public static ApiKeyDto from(ApiKey apiKey) {
        return new ApiKeyDto(
                apiKey.id(),
                apiKey.name(),
                apiKey.keyPrefix(),
                apiKey.status(),
                apiKey.rateLimit(),
                apiKey.expiresAt(),
                apiKey.lastUsedAt(),
                apiKey.createdAt(),
                apiKey.createdBy()
        );
    }
}
