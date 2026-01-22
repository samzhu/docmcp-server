package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/**
 * API Key 實體
 * <p>
 * 用於 API 認證的金鑰，支援過期時間和速率限制設定。
 * 金鑰本身使用 BCrypt 雜湊儲存，只保留前綴用於識別。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("api_keys")
@Value
@EqualsAndHashCode(of = "id")
@ToString(exclude = "keyHash")
public class ApiKey {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 識別名稱 */
    String name;

    /** BCrypt 雜湊後的 Key */
    @Column("key_hash")
    String keyHash;

    /** Key 前綴（如 "dmcp_a1b2"） */
    @Column("key_prefix")
    String keyPrefix;

    /** 狀態（active, revoked, expired） */
    ApiKeyStatus status;

    /** 每小時請求上限 */
    @Column("rate_limit")
    Integer rateLimit;

    /** 過期時間 */
    @Column("expires_at")
    OffsetDateTime expiresAt;

    /** 最後使用時間 */
    @Column("last_used_at")
    @With
    OffsetDateTime lastUsedAt;

    /** 建立者 */
    @Column("created_by")
    String createdBy;

    /** 樂觀鎖定版本號（null 表示新實體） */
    @Version
    @With
    Long version;

    /** 建立時間（由資料庫 DEFAULT 設定） */
    @Column("created_at")
    @With
    OffsetDateTime createdAt;

    /** 更新時間（由資料庫 DEFAULT 設定） */
    @Column("updated_at")
    @With
    OffsetDateTime updatedAt;

    /**
     * 建立新的 API Key（狀態為 ACTIVE）
     * <p>
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id        應用層生成的 TSID
     * @param name      識別名稱
     * @param keyHash   BCrypt 雜湊後的 Key
     * @param keyPrefix Key 前綴
     * @param rateLimit 每小時請求上限
     * @param expiresAt 過期時間
     * @param createdBy 建立者
     * @return 新的 API Key 實例
     */
    public static ApiKey create(String id, String name, String keyHash, String keyPrefix,
                                 Integer rateLimit, OffsetDateTime expiresAt, String createdBy) {
        return new ApiKey(id, name, keyHash, keyPrefix, ApiKeyStatus.ACTIVE,
                rateLimit, expiresAt, null, createdBy, null, null, null);
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
     * 撤銷金鑰
     * <p>
     * 返回一個狀態為 REVOKED 的實例，用於更新現有記錄。
     * 保留 version 以進行樂觀鎖定。
     * </p>
     */
    public ApiKey revoke() {
        return new ApiKey(id, name, keyHash, keyPrefix, ApiKeyStatus.REVOKED,
                rateLimit, expiresAt, lastUsedAt, createdBy, this.version, createdAt, updatedAt);
    }
}
