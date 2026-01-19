package io.github.samzhu.docmcp.mcp.dto;

/**
 * resolve_library 工具的回傳結果
 *
 * @param id              函式庫 ID
 * @param name            函式庫名稱
 * @param displayName     顯示名稱
 * @param resolvedVersion 解析後的版本號
 * @param versionId       版本 ID
 * @param status          版本狀態
 */
public record ResolveLibraryResult(
        String id,
        String name,
        String displayName,
        String resolvedVersion,
        String versionId,
        String status
) {
}
