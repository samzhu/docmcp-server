package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.mcp.dto.CodeExampleDto;
import io.github.samzhu.docmcp.mcp.dto.GetCodeExamplesResult;
import io.github.samzhu.docmcp.service.DocumentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * get_code_examples MCP 工具
 * <p>
 * MCP Level 3 - Retrieve 工具，取得指定函式庫的程式碼範例。
 * </p>
 */
@Component
public class GetCodeExamplesTool {

    private static final int DEFAULT_LIMIT = 10;

    private final DocumentService documentService;

    public GetCodeExamplesTool(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 取得函式庫的程式碼範例
     * <p>
     * 此工具會回傳指定函式庫中的程式碼範例，
     * 可依程式語言篩選。
     * </p>
     *
     * @param libraryId 函式庫 ID
     * @param version   版本，預設為最新版
     * @param language  程式語言篩選（如 java、javascript）
     * @param limit     結果數量上限，預設 10
     * @return 程式碼範例結果
     */
    @Tool(name = "get_code_examples",
            description = """
                    取得函式庫的程式碼範例。

                    使用時機：
                    - 當需要查看特定函式庫的使用範例時
                    - 當需要特定程式語言的範例程式碼時
                    - 當想學習如何使用某個 API 時

                    回傳：程式碼範例列表，包含程式語言、程式碼內容及說明。
                    """)
    public GetCodeExamplesResult getCodeExamples(
            @ToolParam(description = "函式庫 ID（TSID 格式）")
            String libraryId,
            @ToolParam(description = "版本，預設為最新版", required = false)
            String version,
            @ToolParam(description = "程式語言篩選（如 java、javascript、python）", required = false)
            String language,
            @ToolParam(description = "結果數量上限，預設 10", required = false)
            Integer limit
    ) {
        int actualLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        var examples = documentService.getCodeExamples(libraryId, version, language, actualLimit);

        var exampleDtos = examples.stream()
                .map(CodeExampleDto::from)
                .toList();

        return new GetCodeExamplesResult(exampleDtos, exampleDtos.size(), language);
    }
}
