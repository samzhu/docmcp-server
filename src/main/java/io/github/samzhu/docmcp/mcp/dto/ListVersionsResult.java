package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * list_versions 工具的回傳結果
 *
 * @param libraryId 函式庫 ID
 * @param libraryName 函式庫名稱
 * @param versions  版本列表
 * @param total     總數
 */
public record ListVersionsResult(
        String libraryId,
        String libraryName,
        List<LibraryVersionDto> versions,
        int total
) {
}
