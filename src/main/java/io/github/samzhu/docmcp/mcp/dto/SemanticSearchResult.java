package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * semantic_search 工具回傳結果
 * <p>
 * 語意搜尋的結果，包含匹配的文件區塊列表及總數。
 * </p>
 *
 * @param results   搜尋結果列表
 * @param total     結果總數
 * @param query     原始搜尋查詢
 * @param threshold 使用的相似度閾值
 */
public record SemanticSearchResult(
        List<SearchResultItem> results,
        int total,
        String query,
        double threshold
) {}
