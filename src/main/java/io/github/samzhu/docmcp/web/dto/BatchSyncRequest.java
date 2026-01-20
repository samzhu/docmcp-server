package io.github.samzhu.docmcp.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批次同步請求物件
 * <p>
 * 用於一次建立多個版本並觸發同步。
 * </p>
 *
 * @param versions        要同步的版本列表
 * @param defaultDocsPath 預設文件路徑（會套用到未指定 docsPath 的版本）
 */
public record BatchSyncRequest(
        @NotEmpty(message = "至少需要選擇一個版本")
        @Valid
        List<VersionSyncItem> versions,

        @NotNull(message = "預設文件路徑不可為空")
        String defaultDocsPath
) {
    /**
     * 單一版本同步項目
     *
     * @param tagName  標籤名稱（如 v4.0.1）
     * @param version  版本號（如 4.0.1）
     * @param docsPath 文件路徑（可選，若為 null 則使用 defaultDocsPath）
     */
    public record VersionSyncItem(
            @NotNull(message = "標籤名稱不可為空")
            String tagName,

            @NotNull(message = "版本號不可為空")
            String version,

            String docsPath
    ) {
    }
}
