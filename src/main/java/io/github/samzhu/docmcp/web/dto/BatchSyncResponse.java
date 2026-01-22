package io.github.samzhu.docmcp.web.dto;

import java.util.List;

/**
 * 批次同步回應物件
 * <p>
 * 回傳批次同步的結果，包含成功建立的版本和同步歷史 ID。
 * </p>
 *
 * @param message     回應訊息
 * @param syncedItems 已同步的項目列表
 */
public record BatchSyncResponse(
        String message,
        List<SyncedItem> syncedItems
) {
    /**
     * 單一同步項目結果
     *
     * @param versionId     版本 ID（TSID 格式）
     * @param version       版本號
     * @param syncHistoryId 同步歷史 ID（可用於追蹤同步狀態，TSID 格式）
     */
    public record SyncedItem(
            String versionId,
            String version,
            String syncHistoryId
    ) {
    }

    /**
     * 建立成功回應
     *
     * @param syncedItems 已同步的項目列表
     * @return BatchSyncResponse
     */
    public static BatchSyncResponse success(List<SyncedItem> syncedItems) {
        return new BatchSyncResponse(
                String.format("成功啟動 %d 個版本的同步", syncedItems.size()),
                syncedItems
        );
    }
}
