package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.mcp.dto.SearchDocsResult;
import io.github.samzhu.docmcp.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * search_docs MCP 工具
 * <p>
 * MCP Level 2 - Search 工具，在指定函式庫文件中搜尋關鍵字。
 * 使用 PostgreSQL 全文檢索（tsvector/tsquery）。
 * </p>
 */
@Component
public class SearchDocsTool {

    private static final int DEFAULT_LIMIT = 10;

    private final SearchService searchService;

    public SearchDocsTool(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 在指定函式庫的文件中搜尋關鍵字
     * <p>
     * 此工具使用全文檢索功能，適合搜尋特定的術語、API 名稱或錯誤訊息。
     * 搜尋會同時比對文件標題和內容。
     * </p>
     *
     * @param libraryId 函式庫 ID（從 list_libraries 或 resolve_library 取得）
     * @param query     搜尋關鍵字
     * @param version   版本，預設為最新版
     * @param limit     結果數量上限，預設 10
     * @return 搜尋結果
     */
    @Tool(name = "search_docs",
            description = """
                    在指定函式庫的文件中搜尋關鍵字。

                    使用時機：
                    - 當需要查找特定的 API、類別或方法名稱時
                    - 當需要搜尋特定的錯誤訊息或術語時
                    - 當知道確切的關鍵字要搜尋時

                    注意：此工具使用全文檢索，適合精確的關鍵字搜尋。
                    若需要自然語言查詢，請使用 semantic_search。

                    回傳：匹配的文件列表，包含標題、路徑和內容摘要。
                    """)
    public SearchDocsResult searchDocs(
            @ToolParam(description = "函式庫 ID（UUID 格式）")
            UUID libraryId,
            @ToolParam(description = "搜尋關鍵字")
            String query,
            @ToolParam(description = "版本，預設為最新版", required = false)
            String version,
            @ToolParam(description = "結果數量上限，預設 10", required = false)
            Integer limit
    ) {
        int actualLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        var results = searchService.fullTextSearch(libraryId, version, query, actualLimit);

        return new SearchDocsResult(results, results.size(), query);
    }
}
