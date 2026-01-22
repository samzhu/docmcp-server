package io.github.samzhu.docmcp.mcp.tool.search;

import io.github.samzhu.docmcp.mcp.dto.SemanticSearchResult;
import io.github.samzhu.docmcp.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * semantic_search MCP 工具
 * <p>
 * MCP Level 2 - Search 工具，使用自然語言進行語意搜尋。
 * 基於向量相似度（pgvector）實現。
 * </p>
 */
@Component
public class SemanticSearchTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final double DEFAULT_THRESHOLD = 0.7;

    private final SearchService searchService;

    public SemanticSearchTool(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 使用自然語言語意搜尋文件
     * <p>
     * 此工具將查詢文字轉換為向量，並搜尋語意相似的文件區塊。
     * 適合用自然語言描述問題或需求進行搜尋。
     * </p>
     *
     * @param libraryId 函式庫 ID（從 list_libraries 或 resolve_library 取得）
     * @param query     自然語言查詢
     * @param version   版本，預設為最新版
     * @param limit     結果數量上限，預設 5
     * @param threshold 相似度閾值 0-1，預設 0.7
     * @return 搜尋結果
     */
    @Tool(name = "semantic_search",
            description = """
                    使用自然語言語意搜尋文件。

                    使用時機：
                    - 當用自然語言描述問題或需求時
                    - 當不確定精確的關鍵字時
                    - 當想找「如何做某事」的說明時
                    - 當需要找概念相關但用詞不同的內容時

                    注意：此工具使用向量相似度搜尋，適合語意查詢。
                    若知道精確的關鍵字，請使用 search_docs 以獲得更準確的結果。

                    回傳：語意相似的文件區塊列表，包含相似度分數。
                    """)
    public SemanticSearchResult semanticSearch(
            @ToolParam(description = "函式庫 ID（TSID 格式）")
            String libraryId,
            @ToolParam(description = "自然語言查詢，例如「如何設定資料庫連線」")
            String query,
            @ToolParam(description = "版本，預設為最新版", required = false)
            String version,
            @ToolParam(description = "結果數量上限，預設 5", required = false)
            Integer limit,
            @ToolParam(description = "相似度閾值 0-1，預設 0.7，越高結果越精確", required = false)
            Double threshold
    ) {
        int actualLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;
        double actualThreshold = (threshold != null && threshold >= 0 && threshold <= 1)
                ? threshold : DEFAULT_THRESHOLD;

        var results = searchService.semanticSearch(libraryId, version, query,
                actualLimit, actualThreshold);

        return new SemanticSearchResult(results, results.size(), query, actualThreshold);
    }
}
