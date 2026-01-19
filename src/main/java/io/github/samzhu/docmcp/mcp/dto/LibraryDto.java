package io.github.samzhu.docmcp.mcp.dto;

import io.github.samzhu.docmcp.domain.model.Library;

import java.util.List;

/**
 * 函式庫資料傳輸物件
 * <p>
 * 用於 MCP 工具回傳函式庫資訊。
 * </p>
 *
 * @param id          唯一識別碼
 * @param name        函式庫名稱
 * @param displayName 顯示名稱
 * @param description 描述
 * @param category    分類
 * @param tags        標籤
 */
public record LibraryDto(
        String id,
        String name,
        String displayName,
        String description,
        String category,
        List<String> tags
) {
    /**
     * 從 Library 實體轉換
     */
    public static LibraryDto from(Library library) {
        return new LibraryDto(
                library.id() != null ? library.id().toString() : null,
                library.name(),
                library.displayName(),
                library.description(),
                library.category(),
                library.tags()
        );
    }
}
