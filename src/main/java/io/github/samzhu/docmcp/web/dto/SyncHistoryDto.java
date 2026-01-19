package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.SyncHistory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 同步歷史資料傳輸物件
 * <p>
 * 用於 Web API 回傳同步歷史資訊。
 * </p>
 *
 * @param id                 同步 ID
 * @param versionId          版本 ID
 * @param status             同步狀態
 * @param startedAt          開始時間
 * @param completedAt        完成時間
 * @param documentsProcessed 已處理文件數
 * @param chunksCreated      已建立區塊數
 * @param errorMessage       錯誤訊息
 * @param metadata           額外的元資料
 */
public record SyncHistoryDto(
        UUID id,
        UUID versionId,
        SyncStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer documentsProcessed,
        Integer chunksCreated,
        String errorMessage,
        Map<String, Object> metadata
) {
    /**
     * 從 SyncHistory 實體轉換
     */
    public static SyncHistoryDto from(SyncHistory history) {
        return new SyncHistoryDto(
                history.id(),
                history.versionId(),
                history.status(),
                history.startedAt(),
                history.completedAt(),
                history.documentsProcessed(),
                history.chunksCreated(),
                history.errorMessage(),
                history.metadata()
        );
    }
}
