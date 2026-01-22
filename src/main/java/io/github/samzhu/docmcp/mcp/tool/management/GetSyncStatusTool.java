package io.github.samzhu.docmcp.mcp.tool.management;

import io.github.samzhu.docmcp.mcp.dto.GetSyncStatusResult;
import io.github.samzhu.docmcp.mcp.dto.SyncHistoryDto;
import io.github.samzhu.docmcp.repository.SyncHistoryRepository;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 取得同步狀態工具
 * <p>
 * MCP Level 4 - Management 工具，用於查詢文件同步狀態。
 * 包含最新同步記錄及歷史列表。
 * </p>
 */
@Component
public class GetSyncStatusTool {

    private static final int DEFAULT_LIMIT = 5;

    private final LibraryService libraryService;
    private final SyncHistoryRepository syncHistoryRepository;

    public GetSyncStatusTool(LibraryService libraryService,
                             SyncHistoryRepository syncHistoryRepository) {
        this.libraryService = libraryService;
        this.syncHistoryRepository = syncHistoryRepository;
    }

    /**
     * 取得函式庫的同步狀態與歷史
     * <p>
     * 此工具用於查詢文件同步的進度和結果。
     * AI 助手可用此工具協助使用者了解文件更新狀態。
     * </p>
     *
     * @param libraryName 函式庫名稱
     * @param version     版本號（可選，null 表示最新版本）
     * @return 同步狀態與歷史記錄
     */
    @Tool(name = "get_sync_status",
            description = """
                    取得函式庫的同步狀態與歷史記錄。

                    使用時機：
                    - 當需要查看文件同步是否正在進行中時
                    - 當需要查詢最近的同步結果（成功或失敗）時
                    - 當需要了解已處理的文件數量和區塊數量時

                    回傳：同步狀態（是否執行中）、最新同步記錄、最近的同步歷史。
                    """)
    public GetSyncStatusResult getSyncStatus(
            @ToolParam(description = "函式庫名稱", required = true)
            String libraryName,
            @ToolParam(description = "版本號（可選，不指定則使用最新版本）", required = false)
            String version
    ) {
        // 解析函式庫和版本
        var resolved = libraryService.resolveLibrary(libraryName, version);
        var library = resolved.library();
        var libraryVersion = resolved.version();
        var versionId = libraryVersion.getId();

        // 檢查是否有正在執行的同步任務
        boolean isRunning = syncHistoryRepository.hasRunningSyncTask(versionId);

        // 取得最新同步記錄
        var latestSync = syncHistoryRepository.findLatestByVersionId(versionId)
                .map(SyncHistoryDto::from)
                .orElse(null);

        // 取得同步歷史
        var recentSyncs = syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, DEFAULT_LIMIT)
                .stream()
                .map(SyncHistoryDto::from)
                .toList();

        return new GetSyncStatusResult(
                library.getId(),
                library.getName(),
                versionId,
                libraryVersion.getVersion(),
                isRunning,
                latestSync,
                recentSyncs
        );
    }
}
