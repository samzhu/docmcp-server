package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 同步歷史視圖資料傳輸物件
 * <p>
 * 用於頁面顯示，包含函式庫和版本的完整資訊。
 * </p>
 *
 * @param id                 同步 ID
 * @param versionId          版本 ID
 * @param libraryId          函式庫 ID
 * @param libraryName        函式庫名稱
 * @param libraryDisplayName 函式庫顯示名稱
 * @param versionNumber      版本號
 * @param status             同步狀態
 * @param startedAt          開始時間
 * @param completedAt        完成時間
 * @param duration           執行時間（秒）
 * @param documentsProcessed 已處理文件數
 * @param chunksCreated      已建立區塊數
 * @param errorMessage       錯誤訊息
 * @param metadata           額外的元資料
 */
public record SyncHistoryViewDto(
        String id,
        String versionId,
        String libraryId,
        String libraryName,
        String libraryDisplayName,
        String versionNumber,
        SyncStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long duration,
        Integer documentsProcessed,
        Integer chunksCreated,
        String errorMessage,
        Map<String, Object> metadata
) {
    /**
     * 從 SyncHistory、LibraryVersion、Library 實體轉換
     */
    public static SyncHistoryViewDto from(SyncHistory history, LibraryVersion version, Library library) {
        Long durationSeconds = null;
        if (history.getStartedAt() != null && history.getCompletedAt() != null) {
            durationSeconds = Duration.between(history.getStartedAt(), history.getCompletedAt()).getSeconds();
        }

        return new SyncHistoryViewDto(
                history.getId(),
                history.getVersionId(),
                library != null ? library.getId() : null,
                library != null ? library.getName() : null,
                library != null ? library.getDisplayName() : "Unknown Library",
                version != null ? version.getVersion() : "Unknown Version",
                history.getStatus(),
                history.getStartedAt(),
                history.getCompletedAt(),
                durationSeconds,
                history.getDocumentsProcessed(),
                history.getChunksCreated(),
                history.getErrorMessage(),
                history.getMetadata()
        );
    }
}
