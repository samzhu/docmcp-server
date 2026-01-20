package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.mcp.dto.GetApiReferenceResult;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 取得 API 參考文件工具
 * <p>
 * MCP Level 2 - Search 工具，用於搜尋特定的 API 參考文件。
 * 支援搜尋類別名稱、方法名稱、註解等。
 * </p>
 */
@Component
public class GetApiReferenceTool {

    private static final int DEFAULT_LIMIT = 10;

    private final LibraryService libraryService;
    private final SearchService searchService;

    public GetApiReferenceTool(LibraryService libraryService, SearchService searchService) {
        this.libraryService = libraryService;
        this.searchService = searchService;
    }

    /**
     * 搜尋特定的 API 參考文件
     * <p>
     * 此工具用於搜尋類別名稱、方法名稱、註解等 API 相關文件。
     * AI 助手可用此工具協助使用者查找特定 API 的使用方式。
     * </p>
     *
     * @param libraryName 函式庫名稱
     * @param apiName     API 名稱（類別名稱、方法名稱等）
     * @param version     版本號（可選，null 表示最新版本）
     * @return API 參考文件搜尋結果
     */
    @Tool(name = "get_api_reference",
            description = """
                    搜尋特定的 API 參考文件。

                    使用時機：
                    - 當需要查找特定類別或方法的文件時
                    - 當需要了解某個 API 的用法和參數時
                    - 當需要查找特定註解或介面的說明時

                    回傳：包含該 API 的文件列表，包含標題、路徑和相關片段。
                    """)
    public GetApiReferenceResult getApiReference(
            @ToolParam(description = "函式庫名稱", required = true)
            String libraryName,
            @ToolParam(description = "API 名稱（類別名稱、方法名稱、註解等）", required = true)
            String apiName,
            @ToolParam(description = "版本號（可選，不指定則使用最新版本）", required = false)
            String version
    ) {
        // 解析函式庫和版本
        var resolved = libraryService.resolveLibrary(libraryName, version);
        var library = resolved.library();
        var libraryVersion = resolved.version();

        // 使用全文搜尋查找 API 參考
        var searchResults = searchService.fullTextSearch(
                library.id(),
                libraryVersion.version(),
                apiName,
                DEFAULT_LIMIT
        );

        // 轉換搜尋結果
        var results = searchResults.stream()
                .map(item -> new GetApiReferenceResult.ApiReferenceItem(
                        item.documentId().toString(),
                        item.title(),
                        item.path(),
                        item.content(),
                        item.score()
                ))
                .toList();

        return new GetApiReferenceResult(
                library.id().toString(),
                library.name(),
                libraryVersion.version(),
                apiName,
                !results.isEmpty(),
                results
        );
    }
}
