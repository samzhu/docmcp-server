package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.service.SyncService;
import io.github.samzhu.docmcp.web.dto.SyncHistoryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 同步狀態 REST API
 * <p>
 * 提供同步歷史查詢功能。
 * </p>
 */
@RestController
@RequestMapping("/api/sync")
public class SyncApiController {

    private final SyncService syncService;

    public SyncApiController(SyncService syncService) {
        this.syncService = syncService;
    }

    /**
     * 取得同步歷史
     *
     * @param libraryId 函式庫 ID（可選，用於查詢特定函式庫的版本）
     * @param versionId 版本 ID（可選，直接指定版本）
     * @param limit     結果數量上限（預設 10）
     * @return 同步歷史列表
     */
    @GetMapping("/history")
    public List<SyncHistoryDto> getSyncHistory(
            @RequestParam(required = false) UUID libraryId,
            @RequestParam(required = false) UUID versionId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 如果指定了 versionId，直接使用；否則查詢所有
        UUID targetVersionId = versionId;

        List<SyncHistory> histories = syncService.getSyncHistory(targetVersionId, limit);

        return histories.stream()
                .map(SyncHistoryDto::from)
                .toList();
    }

    /**
     * 取得單一同步記錄
     *
     * @param id 同步 ID
     * @return 同步歷史
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncHistoryDto> getSyncStatus(@PathVariable UUID id) {
        return syncService.getSyncStatus(id)
                .map(SyncHistoryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
