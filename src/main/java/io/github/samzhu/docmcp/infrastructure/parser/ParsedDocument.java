package io.github.samzhu.docmcp.infrastructure.parser;

import java.util.List;
import java.util.Map;

/**
 * 解析後的文件
 *
 * @param title      文件標題
 * @param content    文件內容（純文字或 Markdown）
 * @param codeBlocks 程式碼區塊列表
 * @param metadata   額外的元資料
 */
public record ParsedDocument(
        String title,
        String content,
        List<CodeBlock> codeBlocks,
        Map<String, Object> metadata
) {
    /**
     * 程式碼區塊
     *
     * @param language    程式語言
     * @param code        程式碼內容
     * @param description 描述（區塊前的文字）
     * @param startLine   起始行號
     * @param endLine     結束行號
     */
    public record CodeBlock(
            String language,
            String code,
            String description,
            int startLine,
            int endLine
    ) {}
}
