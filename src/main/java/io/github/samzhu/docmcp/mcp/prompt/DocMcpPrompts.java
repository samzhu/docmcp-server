package io.github.samzhu.docmcp.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DocMCP Prompts 提供者
 * <p>
 * 提供預定義的提示模板，幫助 AI 助手更有效地使用 DocMCP 的文件搜尋功能。
 * 這些 Prompts 會在 AI 助手的 UI 中以斜線命令形式呈現（如 /docmcp_explain_library）。
 * </p>
 * <p>
 * 提供的 Prompts：
 * <ul>
 *   <li>docmcp_explain_library - 解釋函式庫的核心概念</li>
 *   <li>docmcp_search_usage - 搜尋特定功能的使用方式</li>
 *   <li>docmcp_compare_versions - 比較兩個版本的差異</li>
 *   <li>docmcp_generate_example - 根據 API 生成程式碼範例</li>
 *   <li>docmcp_troubleshoot - 根據錯誤訊息提供解決方案</li>
 * </ul>
 * </p>
 */
@Component
public class DocMcpPrompts {

    /**
     * 解釋函式庫的核心概念
     * <p>
     * 適用場景：開發者首次接觸某個函式庫時，需要快速了解核心概念、術語和入門步驟。
     * </p>
     *
     * @param library 函式庫名稱（如 spring-boot、react）
     * @param version 版本號，不填則使用最新版
     * @return 包含引導提示的 GetPromptResult
     */
    @McpPrompt(
            name = "docmcp_explain_library",
            description = "解釋函式庫的核心概念、術語和快速入門步驟。適合首次學習某個函式庫時使用。")
    public GetPromptResult explainLibrary(
            @McpArg(name = "library", description = "函式庫名稱（如 spring-boot、react）", required = true)
            String library,
            @McpArg(name = "version", description = "版本號，不填則使用最新版", required = false)
            String version
    ) {
        // 處理可選的版本參數
        String versionText = isBlank(version) ? "最新版本" : version;

        // 生成提示模板，引導 AI 使用 search_docs 工具
        String prompt = """
                請根據 %s %s 的官方文件，解釋：

                1. **核心用途**：這個函式庫解決什麼問題？
                2. **關鍵概念**：列出 3-5 個必須理解的核心術語
                3. **快速入門**：最簡單的使用步驟（含程式碼範例）
                4. **常見模式**：開發者最常用的 2-3 種使用模式

                請使用 search_docs 工具搜尋相關文件，確保資訊是當前版本的。
                """.formatted(library, versionText);

        return createPromptResult("解釋 " + library, prompt);
    }

    /**
     * 搜尋特定功能的使用方式
     * <p>
     * 適用場景：開發者想了解如何在某個函式庫中使用特定功能（如「如何在 Spring Boot 中配置 CORS？」）。
     * </p>
     *
     * @param library 函式庫名稱
     * @param topic   想了解的功能或主題（如 CORS 配置、資料庫連線）
     * @param version 版本號，不填則使用最新版
     * @return 包含引導提示的 GetPromptResult
     */
    @McpPrompt(
            name = "docmcp_search_usage",
            description = "搜尋如何使用某個功能或 API。會搜尋文件並提供具體程式碼範例。")
    public GetPromptResult searchUsage(
            @McpArg(name = "library", description = "函式庫名稱", required = true)
            String library,
            @McpArg(name = "topic", description = "想了解的功能或主題（如 CORS 配置、資料庫連線）", required = true)
            String topic,
            @McpArg(name = "version", description = "版本號，不填則使用最新版", required = false)
            String version
    ) {
        // 處理可選的版本參數
        String versionText = isBlank(version) ? "最新版本" : version;

        // 生成提示模板，引導 AI 使用 search_docs 和 get_code_examples 工具
        String prompt = """
                我想了解如何在 %s %s 中使用 %s。

                請：
                1. 使用 search_docs 搜尋「%s」相關文件
                2. 使用 get_code_examples 取得程式碼範例
                3. 提供完整的實作步驟和範例程式碼
                4. 說明常見的配置選項和注意事項

                確保程式碼適用於指定版本。
                """.formatted(library, versionText, topic, topic);

        return createPromptResult("搜尋 " + library + " 中 " + topic + " 的使用方式", prompt);
    }

    /**
     * 比較兩個版本的差異
     * <p>
     * 適用場景：升級決策、遷移規劃時，需要了解版本間的重大變更和遷移步驟。
     * </p>
     *
     * @param library     函式庫名稱
     * @param fromVersion 目前使用的版本
     * @param toVersion   目標版本，不填則比較最新版
     * @return 包含引導提示的 GetPromptResult
     */
    @McpPrompt(
            name = "docmcp_compare_versions",
            description = "比較函式庫兩個版本之間的差異。幫助決定是否升級以及需要注意的變更。")
    public GetPromptResult compareVersions(
            @McpArg(name = "library", description = "函式庫名稱", required = true)
            String library,
            @McpArg(name = "from_version", description = "目前使用的版本", required = true)
            String fromVersion,
            @McpArg(name = "to_version", description = "目標版本，不填則比較最新版", required = false)
            String toVersion
    ) {
        // 處理可選的目標版本參數
        String toVersionText = isBlank(toVersion) ? "最新版本" : toVersion;

        // 生成提示模板，引導 AI 搜尋遷移相關文件
        String prompt = """
                請比較 %s 從 %s 到 %s 的變更：

                1. **重大變更（Breaking Changes）**：哪些 API 有不相容的變更？
                2. **新功能**：新增了哪些值得注意的功能？
                3. **棄用項目**：哪些功能被標記為棄用？
                4. **遷移步驟**：升級需要做哪些修改？

                請使用 search_docs 搜尋「migration」、「upgrade」、「changelog」相關文件。
                如果有 get_migration_guide 工具可用，也請使用它。
                """.formatted(library, fromVersion, toVersionText);

        return createPromptResult("比較 " + library + " " + fromVersion + " 與 " + toVersionText + " 的差異", prompt);
    }

    /**
     * 根據 API 或功能生成程式碼範例
     * <p>
     * 適用場景：學習新 API 時，需要完整可執行的程式碼範例。
     * </p>
     *
     * @param library      函式庫名稱
     * @param apiOrFeature API 名稱或功能（如 @RestController、useState）
     * @param language     程式語言（如 java、kotlin、typescript）
     * @param version      版本號
     * @return 包含引導提示的 GetPromptResult
     */
    @McpPrompt(
            name = "docmcp_generate_example",
            description = "根據指定的 API 或功能生成完整的程式碼範例。包含必要的匯入、配置和使用方式。")
    public GetPromptResult generateExample(
            @McpArg(name = "library", description = "函式庫名稱", required = true)
            String library,
            @McpArg(name = "api_or_feature", description = "API 名稱或功能（如 @RestController、useState）", required = true)
            String apiOrFeature,
            @McpArg(name = "language", description = "程式語言（如 java、kotlin、typescript）", required = false)
            String language,
            @McpArg(name = "version", description = "版本號", required = false)
            String version
    ) {
        // 處理可選參數
        String versionText = isBlank(version) ? "最新版本" : version;
        String languageText = isBlank(language) ? "函式庫預設語言" : language;

        // 生成提示模板，引導 AI 使用 get_code_examples 工具
        String prompt = """
                請為 %s %s 的 %s 生成程式碼範例。

                要求：
                1. 使用 get_code_examples 搜尋現有範例作為參考
                2. 提供**完整可執行**的程式碼（包含 import 語句）
                3. 加入必要的註解說明
                4. 如果有多種使用方式，展示最常用的 2-3 種
                5. 語言：%s

                範例應該能夠直接複製使用。
                """.formatted(library, versionText, apiOrFeature, languageText);

        return createPromptResult("生成 " + library + " " + apiOrFeature + " 的範例", prompt);
    }

    /**
     * 根據錯誤訊息提供解決方案
     * <p>
     * 適用場景：開發者遇到錯誤時，需要根據錯誤訊息快速找到解決方案。
     * </p>
     *
     * @param library      函式庫名稱
     * @param errorOrIssue 錯誤訊息或問題描述
     * @param version      版本號
     * @return 包含引導提示的 GetPromptResult
     */
    @McpPrompt(
            name = "docmcp_troubleshoot",
            description = "根據錯誤訊息或問題描述，搜尋文件並提供解決方案。")
    public GetPromptResult troubleshoot(
            @McpArg(name = "library", description = "函式庫名稱", required = true)
            String library,
            @McpArg(name = "error_or_issue", description = "錯誤訊息或問題描述", required = true)
            String errorOrIssue,
            @McpArg(name = "version", description = "版本號", required = false)
            String version
    ) {
        // 處理可選的版本參數
        String versionText = isBlank(version) ? "最新版本" : version;

        // 生成提示模板，引導 AI 搜尋相關的故障排除文件
        String prompt = """
                我在使用 %s %s 時遇到以下問題：

                %s

                請：
                1. 使用 search_docs 搜尋相關的錯誤處理文件
                2. 分析可能的原因（列出 2-3 個最可能的原因）
                3. 提供具體的解決步驟
                4. 如果是配置問題，提供正確的配置範例
                5. 說明如何避免這個問題再次發生

                優先參考官方文件的故障排除章節。
                """.formatted(library, versionText, errorOrIssue);

        return createPromptResult("排除 " + library + " 故障", prompt);
    }

    /**
     * 建立 GetPromptResult
     *
     * @param description 提示描述
     * @param promptText  提示文字
     * @return GetPromptResult 實例
     */
    private GetPromptResult createPromptResult(String description, String promptText) {
        return new GetPromptResult(
                description,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    /**
     * 檢查字串是否為空或只包含空白
     *
     * @param str 要檢查的字串
     * @return 如果為 null、空字串或只包含空白則回傳 true
     */
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
