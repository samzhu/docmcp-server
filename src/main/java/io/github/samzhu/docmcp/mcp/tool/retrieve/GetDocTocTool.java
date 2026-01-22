package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.mcp.dto.GetDocTocResult;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 取得文件目錄工具
 * <p>
 * MCP Level 3 - Retrieve 工具，用於取得文件庫的目錄結構。
 * 將文件依路徑結構組織成樹狀目錄。
 * </p>
 */
@Component
public class GetDocTocTool {

    private static final int DEFAULT_MAX_DEPTH = 3;

    private final LibraryService libraryService;
    private final DocumentRepository documentRepository;

    public GetDocTocTool(LibraryService libraryService, DocumentRepository documentRepository) {
        this.libraryService = libraryService;
        this.documentRepository = documentRepository;
    }

    /**
     * 取得文件庫的目錄結構
     * <p>
     * 此工具用於取得函式庫文件的目錄結構。
     * AI 助手可用此工具協助使用者瀏覽文件結構。
     * </p>
     *
     * @param libraryName 函式庫名稱
     * @param version     版本號（可選，null 表示最新版本）
     * @param maxDepth    最大深度（可選，預設 3）
     * @return 文件目錄結構
     */
    @Tool(name = "get_doc_toc",
            description = """
                    取得文件庫的目錄結構（Table of Contents）。

                    使用時機：
                    - 當需要了解文件的整體結構時
                    - 當需要瀏覽特定主題的相關文件時
                    - 當需要幫助使用者找到特定文件時

                    回傳：樹狀目錄結構，包含文件標題、路徑和層級。
                    """)
    public GetDocTocResult getDocToc(
            @ToolParam(description = "函式庫名稱", required = true)
            String libraryName,
            @ToolParam(description = "版本號（可選，不指定則使用最新版本）", required = false)
            String version,
            @ToolParam(description = "最大深度（可選，預設 3）", required = false)
            Integer maxDepth
    ) {
        // 解析函式庫和版本
        var resolved = libraryService.resolveLibrary(libraryName, version);
        var library = resolved.library();
        var libraryVersion = resolved.version();

        // 取得所有文件（依路徑排序）
        var documents = documentRepository.findByVersionIdOrderByPathAsc(libraryVersion.getId());

        // 設定最大深度
        int depth = maxDepth != null && maxDepth > 0 ? maxDepth : DEFAULT_MAX_DEPTH;

        // 建立目錄結構
        var entries = buildTocEntries(documents, depth);

        return new GetDocTocResult(
                library.getId(),
                library.getName(),
                libraryVersion.getVersion(),
                entries,
                documents.size()
        );
    }

    /**
     * 建立目錄條目列表
     */
    private List<GetDocTocResult.TocEntry> buildTocEntries(List<Document> documents, int maxDepth) {
        List<GetDocTocResult.TocEntry> entries = new ArrayList<>();

        for (Document doc : documents) {
            // 計算文件深度
            int docDepth = calculateDepth(doc.getPath());

            // 只加入深度符合限制的文件
            if (docDepth <= maxDepth) {
                entries.add(new GetDocTocResult.TocEntry(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getPath(),
                        docDepth,
                        List.of()  // 簡化實作：不建立巢狀結構
                ));
            }
        }

        return entries;
    }

    /**
     * 計算路徑深度
     */
    private int calculateDepth(String path) {
        if (path == null || path.isBlank()) {
            return 0;
        }
        // 計算路徑分隔符數量
        return (int) path.chars().filter(c -> c == '/').count();
    }
}
