package io.github.samzhu.docmcp.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocMcpPrompts 單元測試
 * <p>
 * 測試 MCP Prompts 的生成邏輯，確保每個 Prompt 方法：
 * - 正確生成包含所需資訊的提示模板
 * - 可選參數為 null 時使用預設值
 * - 生成的訊息角色正確
 * </p>
 */
class DocMcpPromptsTest {

    private DocMcpPrompts prompts;

    @BeforeEach
    void setUp() {
        prompts = new DocMcpPrompts();
    }

    @Nested
    @DisplayName("docmcp_explain_library - 解釋函式庫")
    class ExplainLibraryTests {

        @Test
        @DisplayName("應生成包含函式庫名稱和版本的提示")
        void shouldGeneratePromptWithLibraryAndVersion() {
            // 測試帶版本的情況
            var result = prompts.explainLibrary("spring-boot", "3.2.0");

            assertThat(result).isNotNull();
            assertThat(result.description()).contains("spring-boot");
            assertThat(result.messages()).hasSize(1);
            assertThat(result.messages().getFirst().role()).isEqualTo(Role.USER);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("spring-boot")
                    .contains("3.2.0")
                    .contains("search_docs");
        }

        @Test
        @DisplayName("版本為 null 時應使用最新版本描述")
        void shouldUseLatestVersionWhenVersionIsNull() {
            // 測試不帶版本的情況
            var result = prompts.explainLibrary("react", null);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("react")
                    .contains("最新版本");
        }

        @Test
        @DisplayName("版本為空白時應使用最新版本描述")
        void shouldUseLatestVersionWhenVersionIsBlank() {
            // 測試版本為空白字串的情況
            var result = prompts.explainLibrary("vue", "  ");

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("vue")
                    .contains("最新版本");
        }
    }

    @Nested
    @DisplayName("docmcp_search_usage - 搜尋功能使用方式")
    class SearchUsageTests {

        @Test
        @DisplayName("應生成包含函式庫和主題的提示")
        void shouldGeneratePromptWithLibraryAndTopic() {
            // 測試搜尋特定主題
            var result = prompts.searchUsage("spring-boot", "CORS 配置", "3.2.0");

            assertThat(result).isNotNull();
            assertThat(result.messages()).hasSize(1);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("spring-boot")
                    .contains("CORS 配置")
                    .contains("3.2.0")
                    .contains("search_docs")
                    .contains("get_code_examples");
        }

        @Test
        @DisplayName("版本為 null 時應使用最新版本")
        void shouldUseLatestVersionWhenVersionIsNull() {
            // 測試不帶版本的情況
            var result = prompts.searchUsage("react", "useState", null);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("react")
                    .contains("useState")
                    .contains("最新版本");
        }
    }

    @Nested
    @DisplayName("docmcp_compare_versions - 比較版本差異")
    class CompareVersionsTests {

        @Test
        @DisplayName("應生成包含來源和目標版本的提示")
        void shouldGeneratePromptWithFromAndToVersions() {
            // 測試比較兩個指定版本
            var result = prompts.compareVersions("spring-boot", "2.7.0", "3.2.0");

            assertThat(result).isNotNull();

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("spring-boot")
                    .contains("2.7.0")
                    .contains("3.2.0")
                    .contains("migration")
                    .contains("Breaking Changes");
        }

        @Test
        @DisplayName("目標版本為 null 時應比較到最新版本")
        void shouldCompareToLatestWhenToVersionIsNull() {
            // 測試比較到最新版本
            var result = prompts.compareVersions("react", "17.0.0", null);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("react")
                    .contains("17.0.0")
                    .contains("最新版本");
        }
    }

    @Nested
    @DisplayName("docmcp_generate_example - 生成程式碼範例")
    class GenerateExampleTests {

        @Test
        @DisplayName("應生成包含 API 和語言的提示")
        void shouldGeneratePromptWithApiAndLanguage() {
            // 測試生成範例
            var result = prompts.generateExample("spring-boot", "@RestController", "java", "3.2.0");

            assertThat(result).isNotNull();

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("spring-boot")
                    .contains("@RestController")
                    .contains("java")
                    .contains("3.2.0")
                    .contains("get_code_examples");
        }

        @Test
        @DisplayName("語言和版本為 null 時應使用預設值")
        void shouldUseDefaultsWhenOptionalParamsAreNull() {
            // 測試可選參數為 null 的情況
            var result = prompts.generateExample("react", "useState", null, null);

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("react")
                    .contains("useState")
                    .contains("最新版本");
        }
    }

    @Nested
    @DisplayName("docmcp_troubleshoot - 排除故障")
    class TroubleshootTests {

        @Test
        @DisplayName("應生成包含錯誤訊息的提示")
        void shouldGeneratePromptWithErrorMessage() {
            // 測試故障排除
            var result = prompts.troubleshoot(
                    "spring-boot",
                    "NoSuchBeanDefinitionException: No qualifying bean",
                    "3.2.0"
            );

            assertThat(result).isNotNull();

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("spring-boot")
                    .contains("NoSuchBeanDefinitionException")
                    .contains("3.2.0")
                    .contains("search_docs");
        }

        @Test
        @DisplayName("版本為 null 時應使用最新版本")
        void shouldUseLatestVersionWhenVersionIsNull() {
            // 測試不帶版本的情況
            var result = prompts.troubleshoot(
                    "react",
                    "Error: Invalid hook call",
                    null
            );

            var content = extractTextContent(result);
            assertThat(content)
                    .contains("react")
                    .contains("Invalid hook call")
                    .contains("最新版本");
        }
    }

    /**
     * 從 GetPromptResult 中提取文字內容
     *
     * @param result Prompt 結果
     * @return 文字內容
     */
    private String extractTextContent(GetPromptResult result) {
        var firstMessage = result.messages().getFirst();
        if (firstMessage.content() instanceof TextContent textContent) {
            return textContent.text();
        }
        return firstMessage.content().toString();
    }
}
