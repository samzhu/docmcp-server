package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * search_docs 工具回傳結果
 * <p>
 * 全文檢索的結果，包含匹配的文件列表及總數。
 * </p>
 *
 * @param results 搜尋結果列表
 * @param total   結果總數
 * @param query   原始搜尋查詢
 */
public record SearchDocsResult(
        List<SearchResultItem> results,
        int total,
        String query
) {}
