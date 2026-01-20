package io.github.samzhu.docmcp.mcp.tool.management;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.mcp.dto.LibraryVersionDto;
import io.github.samzhu.docmcp.mcp.dto.ListVersionsResult;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 列出版本工具
 * <p>
 * MCP Level 4 - Management 工具，用於管理函式庫版本。
 * 列出指定函式庫的所有可用版本，支援過濾棄用版本。
 * </p>
 */
@Component
public class ListVersionsTool {

    private final LibraryService libraryService;

    public ListVersionsTool(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * 列出指定函式庫的所有可用版本
     * <p>
     * 此工具用於查詢函式庫的版本資訊，包含版本號、狀態、發布日期等。
     * AI 助手可用此工具幫助使用者選擇適合的版本。
     * </p>
     *
     * @param libraryId         函式庫 ID
     * @param includeDeprecated 是否包含已棄用版本（預設 false）
     * @return 版本列表及相關資訊
     */
    @Tool(name = "list_versions",
            description = """
                    列出指定函式庫的所有可用版本。

                    使用時機：
                    - 當需要查看某個函式庫有哪些版本可用時
                    - 當需要協助使用者選擇適合的版本時
                    - 當需要了解版本狀態（活躍、已棄用、EOL）時

                    回傳：版本列表，包含版本號、狀態、發布日期等資訊。
                    """)
    public ListVersionsResult listVersions(
            @ToolParam(description = "函式庫 ID", required = true)
            String libraryId,
            @ToolParam(description = "是否包含已棄用版本（預設 false）", required = false)
            Boolean includeDeprecated
    ) {
        var uuid = UUID.fromString(libraryId);
        var library = libraryService.getLibraryById(uuid);
        var versions = libraryService.getLibraryVersionsById(uuid);

        // 預設不包含棄用版本
        boolean includeAll = includeDeprecated != null && includeDeprecated;

        var filteredVersions = versions.stream()
                .filter(v -> includeAll || v.status() == VersionStatus.ACTIVE)
                .map(LibraryVersionDto::from)
                .toList();

        return new ListVersionsResult(
                libraryId,
                library.name(),
                filteredVersions,
                filteredVersions.size()
        );
    }
}
