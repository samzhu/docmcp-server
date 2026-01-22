package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 同步歷史實體
 * <p>
 * 記錄文件同步作業的執行歷史，包含狀態、處理數量及錯誤訊息。
 * 可用於追蹤同步進度及偵錯。
 * </p>
 * <p>
 * 使用 @Value 實現 Immutable Entity，@Version 進行樂觀鎖定。
 * version = null 表示新實體（執行 INSERT），version 有值表示既有實體（執行 UPDATE）。
 * </p>
 */
@Table("sync_history")
@Value
@EqualsAndHashCode(of = "id")
public class SyncHistory {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    String id;

    /** 所屬版本 ID（TSID 格式） */
    @Column("version_id")
    String versionId;

    /** 同步狀態（PENDING、RUNNING、SUCCESS、FAILED） */
    SyncStatus status;

    /** 開始時間 */
    @Column("started_at")
    OffsetDateTime startedAt;

    /** 完成時間 */
    @Column("completed_at")
    OffsetDateTime completedAt;

    /** 已處理文件數 */
    @Column("documents_processed")
    Integer documentsProcessed;

    /** 已建立區塊數 */
    @Column("chunks_created")
    Integer chunksCreated;

    /** 錯誤訊息 */
    @Column("error_message")
    @Size(max = 5000)
    String errorMessage;

    /** 額外的元資料 */
    Map<String, Object> metadata;

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
     * 建立新的同步歷史（狀態為 PENDING）
     * <p>
     * createdAt 設為 null，由資料庫 DEFAULT CURRENT_TIMESTAMP 設定。
     * startedAt 在應用層設定，記錄同步開始時間。
     * version = null 讓 Spring Data JDBC 判斷為新實體（執行 INSERT）
     * </p>
     *
     * @param id        應用層生成的 TSID
     * @param versionId 所屬版本 ID
     * @return 新的同步歷史實例
     */
    public static SyncHistory createPending(String id, String versionId) {
        return new SyncHistory(id, versionId, SyncStatus.PENDING,
                OffsetDateTime.now(), null, 0, 0, null, Map.of(), null, null, null);
    }
}
