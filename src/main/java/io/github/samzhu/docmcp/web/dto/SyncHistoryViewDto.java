package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

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
        UUID id,
        UUID versionId,
        UUID libraryId,
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
        if (history.startedAt() != null && history.completedAt() != null) {
            durationSeconds = Duration.between(history.startedAt(), history.completedAt()).getSeconds();
        }

        return new SyncHistoryViewDto(
                history.id(),
                history.versionId(),
                library != null ? library.id() : null,
                library != null ? library.name() : null,
                library != null ? library.displayName() : "Unknown Library",
                version != null ? version.version() : "Unknown Version",
                history.status(),
                history.startedAt(),
                history.completedAt(),
                durationSeconds,
                history.documentsProcessed(),
                history.chunksCreated(),
                history.errorMessage(),
                history.metadata()
        );
    }
}
