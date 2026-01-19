package io.github.samzhu.docmcp.mcp.tool.discovery;

import io.github.samzhu.docmcp.mcp.dto.LibraryDto;
import io.github.samzhu.docmcp.mcp.dto.ListLibrariesResult;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 列出函式庫工具
 * <p>
 * MCP Level 1 - Discovery 工具，用於探索可用的函式庫。
 * 支援依分類篩選。
 * </p>
 */
@Component
public class ListLibrariesTool {

    private final LibraryService libraryService;

    public ListLibrariesTool(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * 列出所有可用的文件函式庫
     * <p>
     * 此工具用於探索 DocMCP Server 中已索引的函式庫。
     * AI 助手應在需要查詢特定函式庫文件前先呼叫此工具。
     * </p>
     *
     * @param category 分類篩選（可選），如 "frontend"、"backend"
     * @return 函式庫列表及總數
     */
    @Tool(name = "list_libraries",
            description = """
                    列出所有可用的文件函式庫。

                    使用時機：
                    - 當需要知道有哪些函式庫可以查詢時
                    - 當需要探索特定分類的函式庫時

                    回傳：函式庫列表，包含名稱、描述、分類等資訊。
                    """)
    public ListLibrariesResult listLibraries(
            @ToolParam(description = "依分類篩選（可選），例如 'frontend'、'backend'", required = false)
            String category
    ) {
        var libraries = libraryService.listLibraries(category);
        var libraryDtos = libraries.stream()
                .map(LibraryDto::from)
                .toList();

        return new ListLibrariesResult(libraryDtos, libraryDtos.size());
    }
}
