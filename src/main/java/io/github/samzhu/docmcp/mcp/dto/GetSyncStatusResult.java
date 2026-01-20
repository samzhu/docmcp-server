package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_sync_status 工具的回傳結果
 *
 * @param libraryId    函式庫 ID
 * @param libraryName  函式庫名稱
 * @param versionId    版本 ID
 * @param version      版本號
 * @param isRunning    是否有正在執行的同步任務
 * @param latestSync   最新同步記錄
 * @param recentSyncs  最近的同步歷史
 */
public record GetSyncStatusResult(
        String libraryId,
        String libraryName,
        String versionId,
        String version,
        boolean isRunning,
        SyncHistoryDto latestSync,
        List<SyncHistoryDto> recentSyncs
) {
}
