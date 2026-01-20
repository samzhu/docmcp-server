package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_api_reference 工具的回傳結果
 *
 * @param libraryId   函式庫 ID
 * @param libraryName 函式庫名稱
 * @param version     版本號
 * @param apiName     API 名稱
 * @param found       是否找到
 * @param results     搜尋結果
 */
public record GetApiReferenceResult(
        String libraryId,
        String libraryName,
        String version,
        String apiName,
        boolean found,
        List<ApiReferenceItem> results
) {
    /**
     * API 參考項目
     *
     * @param documentId 文件 ID
     * @param title      文件標題
     * @param path       文件路徑
     * @param snippet    包含 API 的片段
     * @param score      相關度分數
     */
    public record ApiReferenceItem(
            String documentId,
            String title,
            String path,
            String snippet,
            double score
    ) {}
}
