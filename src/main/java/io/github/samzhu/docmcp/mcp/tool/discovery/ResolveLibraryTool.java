package io.github.samzhu.docmcp.mcp.tool.discovery;

import io.github.samzhu.docmcp.mcp.dto.ResolveLibraryResult;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 解析函式庫工具
 * <p>
 * MCP Level 1 - Discovery 工具，用於解析函式庫名稱和版本，
 * 取得可用於後續查詢的函式庫和版本 ID。
 * </p>
 */
@Component
public class ResolveLibraryTool {

    private final LibraryService libraryService;

    public ResolveLibraryTool(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * 解析函式庫名稱和版本
     * <p>
     * 根據函式庫名稱和可選的版本號，解析出完整的函式庫資訊。
     * 若未指定版本，則使用最新版本。
     * </p>
     *
     * @param library 函式庫名稱（必填）
     * @param version 版本號（可選，null 表示最新版本）
     * @return 解析結果，包含函式庫 ID、版本 ID 等資訊
     */
    @Tool(name = "resolve_library",
            description = """
                    解析函式庫名稱和版本，取得函式庫 ID 和版本資訊。

                    使用時機：
                    - 在搜尋或取得文件內容前，需要先解析函式庫
                    - 當需要確認函式庫是否存在及其可用版本時

                    參數：
                    - library: 函式庫名稱（必填），如 'spring-boot'、'react'
                    - version: 版本號（可選），如 '3.2.0'，不指定則使用最新版本

                    回傳：函式庫 ID、解析後的版本號、版本 ID。
                    """)
    public ResolveLibraryResult resolveLibrary(
            @ToolParam(description = "函式庫名稱，如 'spring-boot'、'react'")
            String library,
            @ToolParam(description = "版本號（可選），如 '3.2.0'。不指定則使用最新版本", required = false)
            String version
    ) {
        var resolved = libraryService.resolveLibrary(library, version);

        return new ResolveLibraryResult(
                resolved.library().getId(),
                resolved.library().getName(),
                resolved.library().getDisplayName(),
                resolved.resolvedVersion(),
                resolved.version().getId(),
                resolved.version().getStatus().name()
        );
    }
}
