package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 同步歷史實體
 * <p>
 * 記錄文件同步作業的執行歷史，包含狀態、處理數量及錯誤訊息。
 * 可用於追蹤同步進度及偵錯。
 * </p>
 *
 * @param id                 唯一識別碼
 * @param versionId          所屬版本 ID
 * @param status             同步狀態（PENDING、RUNNING、SUCCESS、FAILED）
 * @param startedAt          開始時間
 * @param completedAt        完成時間
 * @param documentsProcessed 已處理文件數
 * @param chunksCreated      已建立區塊數
 * @param errorMessage       錯誤訊息
 * @param metadata           額外的元資料
 */
@Table("sync_history")
public record SyncHistory(
        @Id UUID id,
        @Column("version_id") UUID versionId,
        SyncStatus status,
        @Column("started_at") OffsetDateTime startedAt,
        @Column("completed_at") OffsetDateTime completedAt,
        @Column("documents_processed") Integer documentsProcessed,
        @Column("chunks_created") Integer chunksCreated,
        @Column("error_message") String errorMessage,
        Map<String, Object> metadata
) {
    /**
     * 建立新的同步歷史（狀態為 PENDING）
     */
    public static SyncHistory createPending(UUID versionId) {
        return new SyncHistory(null, versionId, SyncStatus.PENDING,
                OffsetDateTime.now(), null, 0, 0, null, Map.of());
    }
}
