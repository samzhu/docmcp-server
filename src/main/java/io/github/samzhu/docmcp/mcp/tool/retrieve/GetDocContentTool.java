package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.domain.exception.DocumentNotFoundException;
import io.github.samzhu.docmcp.mcp.dto.CodeExampleDto;
import io.github.samzhu.docmcp.mcp.dto.DocumentContentDto;
import io.github.samzhu.docmcp.mcp.dto.GetDocContentResult;
import io.github.samzhu.docmcp.service.DocumentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * get_doc_content MCP 工具
 * <p>
 * MCP Level 3 - Retrieve 工具，取得特定文件的完整內容。
 * </p>
 */
@Component
public class GetDocContentTool {

    private final DocumentService documentService;

    public GetDocContentTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 取得文件完整內容
     * <p>
     * 此工具會回傳文件的完整內容，包含文件區塊和程式碼範例。
     * 適合在搜尋到相關文件後，取得完整內容進行深入閱讀。
     * </p>
     *
     * @param documentId          文件 ID（從搜尋結果取得）
     * @param includeCodeExamples 是否包含程式碼範例，預設 true
     * @return 文件內容結果
     * @throws DocumentNotFoundException 若文件不存在
     */
    @Tool(name = "get_doc_content",
            description = """
                    取得文件完整內容。

                    使用時機：
                    - 當透過 search_docs 或 semantic_search 找到相關文件後
                    - 當需要閱讀文件的完整內容時
                    - 當需要查看文件中的程式碼範例時

                    回傳：文件完整內容，包含標題、路徑、內容及程式碼範例。
                    """)
    public GetDocContentResult getDocContent(
            @ToolParam(description = "文件 ID（UUID 格式，從搜尋結果取得）")
            UUID documentId,
            @ToolParam(description = "是否包含程式碼範例，預設 true", required = false)
            Boolean includeCodeExamples
    ) {
        boolean includeExamples = includeCodeExamples == null || includeCodeExamples;

        var content = documentService.getDocumentContent(documentId);

        // 轉換為 DTO
        var chunkDtos = content.chunks().stream()
                .map(chunk -> new DocumentContentDto.ChunkDto(
                        chunk.id(),
                        chunk.chunkIndex(),
                        chunk.content()
                ))
                .toList();

        var documentDto = DocumentContentDto.from(content.document(), chunkDtos);

        List<CodeExampleDto> codeExampleDtos = includeExamples
                ? content.codeExamples().stream()
                        .map(CodeExampleDto::from)
                        .toList()
                : List.of();

        return new GetDocContentResult(documentDto, codeExampleDtos);
    }
}
