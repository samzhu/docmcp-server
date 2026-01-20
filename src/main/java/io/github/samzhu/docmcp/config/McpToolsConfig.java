package io.github.samzhu.docmcp.config;

import io.github.samzhu.docmcp.mcp.tool.discovery.ListLibrariesTool;
import io.github.samzhu.docmcp.mcp.tool.discovery.ResolveLibraryTool;
import io.github.samzhu.docmcp.mcp.tool.management.GetSyncStatusTool;
import io.github.samzhu.docmcp.mcp.tool.management.ListVersionsTool;
import io.github.samzhu.docmcp.mcp.tool.retrieve.GetCodeExamplesTool;
import io.github.samzhu.docmcp.mcp.tool.retrieve.GetDocContentTool;
import io.github.samzhu.docmcp.mcp.tool.retrieve.GetDocTocTool;
import io.github.samzhu.docmcp.mcp.tool.retrieve.GetRelatedDocsTool;
import io.github.samzhu.docmcp.mcp.tool.search.GetApiReferenceTool;
import io.github.samzhu.docmcp.mcp.tool.search.SearchDocsTool;
import io.github.samzhu.docmcp.mcp.tool.search.SemanticSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具配置
 * <p>
 * 註冊所有 MCP 工具至 Spring AI MCP Server。
 * 這些工具會透過 MCP 協議暴露給 AI 助手使用。
 * </p>
 */
@Configuration
public class McpToolsConfig {

    /**
     * 註冊 MCP 工具回調提供者
     * <p>
     * 將標註 @Tool 的方法註冊為 MCP 工具。
     * </p>
     *
     * @param listLibrariesTool    列出函式庫工具
     * @param resolveLibraryTool   解析函式庫工具
     * @param searchDocsTool       全文搜尋工具
     * @param semanticSearchTool   語意搜尋工具
     * @param getApiReferenceTool  取得 API 參考工具
     * @param getDocContentTool    取得文件內容工具
     * @param getCodeExamplesTool  取得程式碼範例工具
     * @param getDocTocTool        取得文件目錄工具
     * @param getRelatedDocsTool   取得相關文件工具
     * @param listVersionsTool     列出版本工具
     * @param getSyncStatusTool    取得同步狀態工具
     * @return 工具回調提供者
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            // Discovery Layer
            ListLibrariesTool listLibrariesTool,
            ResolveLibraryTool resolveLibraryTool,
            // Search Layer
            SearchDocsTool searchDocsTool,
            SemanticSearchTool semanticSearchTool,
            GetApiReferenceTool getApiReferenceTool,
            // Retrieve Layer
            GetDocContentTool getDocContentTool,
            GetCodeExamplesTool getCodeExamplesTool,
            GetDocTocTool getDocTocTool,
            GetRelatedDocsTool getRelatedDocsTool,
            // Management Layer
            ListVersionsTool listVersionsTool,
            GetSyncStatusTool getSyncStatusTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        // Discovery Layer
                        listLibrariesTool, resolveLibraryTool,
                        // Search Layer
                        searchDocsTool, semanticSearchTool, getApiReferenceTool,
                        // Retrieve Layer
                        getDocContentTool, getCodeExamplesTool, getDocTocTool, getRelatedDocsTool,
                        // Management Layer
                        listVersionsTool, getSyncStatusTool
                )
                .build();
    }
}
