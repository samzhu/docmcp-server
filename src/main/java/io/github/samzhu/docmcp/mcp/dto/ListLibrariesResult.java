package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * list_libraries 工具的回傳結果
 *
 * @param libraries 函式庫列表
 * @param total     總數
 */
public record ListLibrariesResult(
        List<LibraryDto> libraries,
        int total
) {
}
