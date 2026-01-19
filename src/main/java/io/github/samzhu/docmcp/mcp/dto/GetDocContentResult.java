package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_doc_content 工具回傳結果
 * <p>
 * 回傳文件的完整內容及相關程式碼範例。
 * </p>
 *
 * @param document     文件內容
 * @param codeExamples 程式碼範例列表（可選）
 */
public record GetDocContentResult(
        DocumentContentDto document,
        List<CodeExampleDto> codeExamples
) {}
