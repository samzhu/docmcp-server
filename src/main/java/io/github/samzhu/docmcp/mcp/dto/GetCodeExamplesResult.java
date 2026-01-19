package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;

/**
 * get_code_examples 工具回傳結果
 * <p>
 * 回傳程式碼範例列表。
 * </p>
 *
 * @param examples 程式碼範例列表
 * @param total    總數
 * @param language 篩選的程式語言（若有）
 */
public record GetCodeExamplesResult(
        List<CodeExampleDto> examples,
        int total,
        String language
) {}
