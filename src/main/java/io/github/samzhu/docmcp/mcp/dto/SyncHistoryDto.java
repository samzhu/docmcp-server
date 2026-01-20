package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.SyncHistory;

import java.time.OffsetDateTime;

/**
 * 同步歷史資料傳輸物件
 * <p>
 * 用於 MCP 工具回傳同步歷史資訊。
 * </p>
 *
 * @param id                 唯一識別碼
 * @param versionId          版本 ID
 * @param status             同步狀態
 * @param startedAt          開始時間
 * @param completedAt        完成時間
 * @param documentsProcessed 已處理文件數
 * @param chunksCreated      已建立區塊數
 * @param errorMessage       錯誤訊息
 */
public record SyncHistoryDto(
        String id,
        String versionId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer documentsProcessed,
        Integer chunksCreated,
        String errorMessage
) {
    /**
     * 從 SyncHistory 實體轉換
     */
    public static SyncHistoryDto from(SyncHistory syncHistory) {
        return new SyncHistoryDto(
                syncHistory.id() != null ? syncHistory.id().toString() : null,
                syncHistory.versionId() != null ? syncHistory.versionId().toString() : null,
                syncHistory.status() != null ? syncHistory.status().name() : null,
                syncHistory.startedAt(),
                syncHistory.completedAt(),
                syncHistory.documentsProcessed(),
                syncHistory.chunksCreated(),
                syncHistory.errorMessage()
        );
    }
}
