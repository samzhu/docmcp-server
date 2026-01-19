package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;

import java.util.List;
import java.util.UUID;

/**
 * 搜尋結果資料傳輸物件
 * <p>
 * 用於 Web API 回傳搜尋結果。
 * </p>
 *
 * @param query     搜尋查詢
 * @param mode      搜尋模式（fulltext, semantic, hybrid）
 * @param total     結果總數
 * @param items     搜尋結果項目列表
 */
public record SearchResultDto(
        String query,
        String mode,
        int total,
        List<SearchResultItemDto> items
) {
    /**
     * 從搜尋結果列表建立
     */
    public static SearchResultDto from(String query, String mode, List<SearchResultItem> items) {
        List<SearchResultItemDto> dtos = items.stream()
                .map(SearchResultItemDto::from)
                .toList();
        return new SearchResultDto(query, mode, dtos.size(), dtos);
    }

    /**
     * 搜尋結果項目 DTO
     */
    public record SearchResultItemDto(
            UUID documentId,
            UUID chunkId,
            String title,
            String path,
            String content,
            double score,
            Integer chunkIndex
    ) {
        public static SearchResultItemDto from(SearchResultItem item) {
            return new SearchResultItemDto(
                    item.documentId(),
                    item.chunkId(),
                    item.title(),
                    item.path(),
                    item.content(),
                    item.score(),
                    item.chunkIndex()
            );
        }
    }
}
