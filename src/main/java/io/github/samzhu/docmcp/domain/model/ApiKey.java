package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Key 實體
 * <p>
 * 用於 API 認證的金鑰，支援過期時間和速率限制設定。
 * 金鑰本身使用 BCrypt 雜湊儲存，只保留前綴用於識別。
 * </p>
 *
 * @param id         唯一識別碼
 * @param name       識別名稱
 * @param keyHash    BCrypt 雜湊後的 Key
 * @param keyPrefix  Key 前綴（如 "dmcp_a1b2"）
 * @param status     狀態（active, revoked, expired）
 * @param rateLimit  每小時請求上限
 * @param expiresAt  過期時間
 * @param lastUsedAt 最後使用時間
 * @param createdAt  建立時間
 * @param createdBy  建立者
 */
@Table("api_keys")
public record ApiKey(
        @Id UUID id,
        String name,
        @Column("key_hash") String keyHash,
        @Column("key_prefix") String keyPrefix,
        ApiKeyStatus status,
        @Column("rate_limit") Integer rateLimit,
        @Column("expires_at") OffsetDateTime expiresAt,
        @Column("last_used_at") OffsetDateTime lastUsedAt,
        @Column("created_at") OffsetDateTime createdAt,
        @Column("created_by") String createdBy
) {
    /**
     * 建立新的 API Key（狀態為 ACTIVE）
     */
    public static ApiKey create(String name, String keyHash, String keyPrefix,
                                 Integer rateLimit, OffsetDateTime expiresAt, String createdBy) {
        return new ApiKey(null, name, keyHash, keyPrefix, ApiKeyStatus.ACTIVE,
                rateLimit, expiresAt, null, null, createdBy);
    }

    /**
     * 檢查是否已過期
     */
    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * 檢查是否有效（狀態為 ACTIVE 且未過期）
     */
    public boolean isValid() {
        return status == ApiKeyStatus.ACTIVE && !isExpired();
    }

    /**
     * 更新最後使用時間
     */
    public ApiKey withLastUsedAt(OffsetDateTime lastUsedAt) {
        return new ApiKey(id, name, keyHash, keyPrefix, status, rateLimit,
                expiresAt, lastUsedAt, createdAt, createdBy);
    }

    /**
     * 撤銷金鑰
     */
    public ApiKey revoke() {
        return new ApiKey(id, name, keyHash, keyPrefix, ApiKeyStatus.REVOKED,
                rateLimit, expiresAt, lastUsedAt, createdAt, createdBy);
    }
}
