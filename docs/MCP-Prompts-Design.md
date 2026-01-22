# MCP Prompts 設計指南

> **版本**：1.0.0
> **最後更新**：2026-01-22
> **狀態**：Draft

---

## 1. 概述

### 1.1 什麼是 MCP Prompts？

MCP Prompts 是 Model Context Protocol 的三大能力之一（Tools、Resources、**Prompts**），提供**預定義的提示模板**，讓使用者在 AI 助手中可以快速選用。

```
┌─────────────────────────────────────────────────────────────┐
│  MCP 三大能力                                                │
├─────────────────────────────────────────────────────────────┤
│  Tools     - AI 可以「執行」的動作（搜尋、取得文件）           │
│  Resources - AI 可以「讀取」的資料（文件內容）                │
│  Prompts   - 使用者可以「選用」的提示模板                     │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Prompts vs Tools vs Resources

| 類型 | 用途 | 觸發者 | 副作用 | 回傳 |
|-----|------|-------|-------|------|
| **Prompts** | 引導互動（模板） | 使用者/UI | 無 | 訊息序列 |
| **Tools** | 執行動作 | Agent | 有 | 執行結果 |
| **Resources** | 提供唯讀資料 | Agent | 無 | 資料內容 |

### 1.3 使用者互動模式

Prompts 設計為**使用者控制**，通常透過以下方式呈現：

- **斜線命令**（如 `/explain_library`）
- **命令選板**（Command Palette）
- **快速操作選單**
- **上下文選單項目**

---

## 2. 設計原則

### 2.1 核心原則（來自 MCP Best Practices）

> **「MCP 是 AI Agent 的使用者介面，要像設計 UI 一樣設計它。」**
> — [philschmid.de/mcp-best-practices](https://www.philschmid.de/mcp-best-practices)

| 原則 | 說明 | 範例 |
|-----|------|------|
| **Outcomes Over Operations** | 設計圍繞使用者目標，而非 API 端點 | `track_latest_order(email)` 而非三個分開的 API |
| **Flatten Arguments** | 使用頂層原始類型，避免複雜巢狀結構 | 明確的 `String`、`Integer` 參數 |
| **Instructions as Context** | 每個文字（描述、錯誤訊息）都是 Agent 的上下文 | 提供具體指引而非通用描述 |
| **Ruthless Curation** | 5-15 個工具/提示，專注單一職責 | 「One server, one job」 |
| **Service-Prefixed Naming** | 採用 `{service}_{action}_{resource}` 命名 | `docmcp_search_usage` |
| **Pagination with Metadata** | 回傳 `has_more`、`next_offset` 而非大量原始資料 | 預設 20-50 筆結果 |

### 2.2 命名最佳實踐

**做：**
```
✅ docmcp_explain_library    - 清晰、可執行
✅ docmcp_search_usage       - 動詞 + 名詞
✅ docmcp_compare_versions   - 描述結果
```

**不做：**
```
❌ get_summarized_error_log_output  - 過長
❌ helper                           - 太模糊
❌ doStuff                          - 無意義
```

### 2.3 參數設計原則

```java
// 好的設計：明確、有約束
@McpArg(name = "library", description = "函式庫名稱（如 spring-boot）", required = true)
String library

@McpArg(name = "version", description = "版本號，不填則使用最新版", required = false)
String version

// 避免：複雜巢狀物件
@McpArg(name = "options", description = "各種選項...")
Map<String, Object> options  // 太複雜，Agent 難以理解
```

---

## 3. Context7 的設計啟發

### 3.1 問題與解決方案

[Context7](https://github.com/upstash/context7) 是一個專注於解決 AI 編程助手文件問題的 MCP Server。

**沒有 Context7 的情況：**
- LLM 依賴過時的訓練資料
- 產生**不存在的 API**（幻覺）
- 無法適應**特定版本**的需求
- 開發者需要頻繁切換分頁查文件

**有 Context7 的情況：**
- 用戶只需加入 `use context7`，自動注入當前版本文件
- 版本專用的程式碼範例
- 減少幻覺，提高程式碼品質

### 3.2 Context7 的工具設計

```
1. resolve-library-id
   - 解析套件名稱為 Context7 相容的 library ID
   - 必須先呼叫此工具，才能使用 query-docs

2. query-docs
   - 查詢最新文件和程式碼範例
   - 需要 library ID 作為參數
```

**關鍵設計理念**：兩步驟流程確保精確匹配，避免模糊查詢。

---

## 4. DocMCP Server Prompts 設計

### 4.1 設計目標

為 DocMCP Server 設計的 Prompts 應該：

1. **減少 AI 幻覺**：透過即時文件查詢確保資訊正確
2. **版本感知**：支援特定版本的文件查詢
3. **開發者友善**：解決常見開發場景的需求
4. **與現有 Tools 協作**：引導 AI 使用 `search_docs`、`get_doc_content` 等工具

### 4.2 建議的 5 個核心 Prompts

#### 4.2.1 `docmcp_explain_library`

**用途**：解釋函式庫的核心概念

**場景**：開發者首次接觸某個函式庫時

```java
@McpPrompt(
    name = "docmcp_explain_library",
    description = "解釋函式庫的核心概念、術語和快速入門步驟。" +
                  "適合首次學習某個函式庫時使用。")
public GetPromptResult explainLibrary(
    @McpArg(name = "library",
            description = "函式庫名稱（如 spring-boot、react）",
            required = true)
    String library,

    @McpArg(name = "version",
            description = "版本號，不填則使用最新版",
            required = false)
    String version
)
```

**生成的提示模板**：
```
請根據 {library} {version} 的官方文件，解釋：

1. **核心用途**：這個函式庫解決什麼問題？
2. **關鍵概念**：列出 3-5 個必須理解的核心術語
3. **快速入門**：最簡單的使用步驟（含程式碼範例）
4. **常見模式**：開發者最常用的 2-3 種使用模式

請使用 search_docs 工具搜尋相關文件，確保資訊是當前版本的。
```

---

#### 4.2.2 `docmcp_search_usage`

**用途**：搜尋特定功能的使用方式

**場景**：「如何在 Spring Boot 中配置 CORS？」

```java
@McpPrompt(
    name = "docmcp_search_usage",
    description = "搜尋如何使用某個功能或 API。" +
                  "會搜尋文件並提供具體程式碼範例。")
public GetPromptResult searchUsage(
    @McpArg(name = "library",
            description = "函式庫名稱",
            required = true)
    String library,

    @McpArg(name = "topic",
            description = "想了解的功能或主題（如 CORS 配置、資料庫連線）",
            required = true)
    String topic,

    @McpArg(name = "version",
            description = "版本號，不填則使用最新版",
            required = false)
    String version
)
```

**生成的提示模板**：
```
我想了解如何在 {library} {version} 中使用 {topic}。

請：
1. 使用 search_docs 搜尋 "{topic}" 相關文件
2. 使用 get_code_examples 取得程式碼範例
3. 提供完整的實作步驟和範例程式碼
4. 說明常見的配置選項和注意事項

確保程式碼適用於指定版本。
```

---

#### 4.2.3 `docmcp_compare_versions`

**用途**：比較兩個版本的差異

**場景**：升級決策、遷移規劃

```java
@McpPrompt(
    name = "docmcp_compare_versions",
    description = "比較函式庫兩個版本之間的差異。" +
                  "幫助決定是否升級以及需要注意的變更。")
public GetPromptResult compareVersions(
    @McpArg(name = "library",
            description = "函式庫名稱",
            required = true)
    String library,

    @McpArg(name = "from_version",
            description = "目前使用的版本",
            required = true)
    String fromVersion,

    @McpArg(name = "to_version",
            description = "目標版本，不填則比較最新版",
            required = false)
    String toVersion
)
```

**生成的提示模板**：
```
請比較 {library} 從 {from_version} 到 {to_version} 的變更：

1. **重大變更（Breaking Changes）**：哪些 API 有不相容的變更？
2. **新功能**：新增了哪些值得注意的功能？
3. **棄用項目**：哪些功能被標記為棄用？
4. **遷移步驟**：升級需要做哪些修改？

請使用 search_docs 搜尋 "migration"、"upgrade"、"changelog" 相關文件。
如果有 get_migration_guide 工具可用，也請使用它。
```

---

#### 4.2.4 `docmcp_generate_example`

**用途**：根據 API 生成程式碼範例

**場景**：學習新 API 時快速上手

```java
@McpPrompt(
    name = "docmcp_generate_example",
    description = "根據指定的 API 或功能生成完整的程式碼範例。" +
                  "包含必要的匯入、配置和使用方式。")
public GetPromptResult generateExample(
    @McpArg(name = "library",
            description = "函式庫名稱",
            required = true)
    String library,

    @McpArg(name = "api_or_feature",
            description = "API 名稱或功能（如 @RestController、useState）",
            required = true)
    String apiOrFeature,

    @McpArg(name = "language",
            description = "程式語言（如 java、kotlin、typescript）",
            required = false)
    String language,

    @McpArg(name = "version",
            description = "版本號",
            required = false)
    String version
)
```

**生成的提示模板**：
```
請為 {library} {version} 的 {api_or_feature} 生成程式碼範例。

要求：
1. 使用 get_code_examples 搜尋現有範例作為參考
2. 提供**完整可執行**的程式碼（包含 import 語句）
3. 加入必要的註解說明
4. 如果有多種使用方式，展示最常用的 2-3 種
5. 語言：{language}

範例應該能夠直接複製使用。
```

---

#### 4.2.5 `docmcp_troubleshoot`

**用途**：根據錯誤訊息提供解決方案

**場景**：開發者遇到問題時

```java
@McpPrompt(
    name = "docmcp_troubleshoot",
    description = "根據錯誤訊息或問題描述，搜尋文件並提供解決方案。")
public GetPromptResult troubleshoot(
    @McpArg(name = "library",
            description = "函式庫名稱",
            required = true)
    String library,

    @McpArg(name = "error_or_issue",
            description = "錯誤訊息或問題描述",
            required = true)
    String errorOrIssue,

    @McpArg(name = "version",
            description = "版本號",
            required = false)
    String version
)
```

**生成的提示模板**：
```
我在使用 {library} {version} 時遇到以下問題：

{error_or_issue}

請：
1. 使用 search_docs 搜尋相關的錯誤處理文件
2. 分析可能的原因（列出 2-3 個最可能的原因）
3. 提供具體的解決步驟
4. 如果是配置問題，提供正確的配置範例
5. 說明如何避免這個問題再次發生

優先參考官方文件的故障排除章節。
```

---

## 5. 實作指南

### 5.1 Spring AI 實作方式

```java
package io.github.samzhu.docmcp.mcp.prompt;

import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.spec.McpSchema.GetPromptResult;
import org.springframework.ai.mcp.spec.McpSchema.PromptMessage;
import org.springframework.ai.mcp.spec.McpSchema.Role;
import org.springframework.ai.mcp.spec.McpSchema.TextContent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DocMCP Prompts 提供者
 * <p>
 * 提供預定義的提示模板，幫助 AI 助手更有效地使用 DocMCP 的文件搜尋功能。
 * </p>
 */
@Component
public class DocMcpPrompts {

    @McpPrompt(
        name = "docmcp_explain_library",
        description = "解釋函式庫的核心概念、術語和快速入門步驟")
    public GetPromptResult explainLibrary(
        @McpArg(name = "library", description = "函式庫名稱", required = true)
        String library,
        @McpArg(name = "version", description = "版本號（可選）", required = false)
        String version
    ) {
        String versionText = version != null && !version.isBlank()
            ? version
            : "最新版本";

        String prompt = """
            請根據 %s %s 的官方文件，解釋：

            1. **核心用途**：這個函式庫解決什麼問題？
            2. **關鍵概念**：列出 3-5 個必須理解的核心術語
            3. **快速入門**：最簡單的使用步驟（含程式碼範例）
            4. **常見模式**：開發者最常用的 2-3 種使用模式

            請使用 search_docs 工具搜尋相關文件，確保資訊是當前版本的。
            """.formatted(library, versionText);

        return new GetPromptResult(
            "解釋 " + library,
            List.of(new PromptMessage(Role.USER, new TextContent(prompt)))
        );
    }

    // ... 其他 Prompts
}
```

### 5.2 配置啟用

確保 `application.yaml` 已啟用 annotation scanner：

```yaml
spring:
  ai:
    mcp:
      server:
        annotation-scanner:
          enabled: true
```

### 5.3 測試方式

```java
@ExtendWith(MockitoExtension.class)
class DocMcpPromptsTest {

    private DocMcpPrompts prompts;

    @BeforeEach
    void setUp() {
        prompts = new DocMcpPrompts();
    }

    @Test
    void explainLibrary_shouldGeneratePromptWithVersion() {
        var result = prompts.explainLibrary("spring-boot", "3.2.0");

        assertThat(result.description()).contains("spring-boot");
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).content().toString())
            .contains("spring-boot")
            .contains("3.2.0");
    }

    @Test
    void explainLibrary_shouldUseLatestWhenVersionIsNull() {
        var result = prompts.explainLibrary("react", null);

        assertThat(result.messages().get(0).content().toString())
            .contains("最新版本");
    }
}
```

---

## 6. 安全考量

根據 [MCP 規格](https://modelcontextprotocol.io/specification/2025-06-18/server/prompts)，實作時應注意：

| 考量 | 措施 |
|-----|------|
| **參數驗證** | 驗證必要參數存在且格式正確 |
| **輸入消毒** | 防止 Prompt Injection 攻擊 |
| **速率限制** | 避免濫用 |
| **存取控制** | 確保只有授權用戶可使用 |
| **內容驗證** | 確保生成的提示不含敏感資訊 |

---

## 7. 參考資料

| 資源 | 連結 |
|------|------|
| MCP Best Practices | https://www.philschmid.de/mcp-best-practices |
| MCP Prompts Specification | https://modelcontextprotocol.io/specification/2025-06-18/server/prompts |
| MCP Prompts Concepts | https://modelcontextprotocol.info/docs/concepts/prompts/ |
| Context7 MCP | https://github.com/upstash/context7 |
| Spring AI MCP Annotations | https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html |
| Speakeasy MCP Prompts | https://www.speakeasy.com/mcp/core-concepts/prompts |
| DevExpress Documentation MCP | https://docs.devexpress.com/GeneralInformation/405551 |
| MCP Prompts Workflow Automation | http://blog.modelcontextprotocol.io/posts/2025-07-29-prompts-for-automation/ |

---

## 變更記錄

| 版本 | 日期 | 變更內容 | 作者 |
|------|------|----------|------|
| 1.0.0 | 2026-01-22 | 初始版本 | Claude |
