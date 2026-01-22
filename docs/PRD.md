# DocMCP Server - Product Requirements Document (PRD)

> **版本**：1.7.0
> **最後更新**：2026-01-22
> **狀態**：Draft
> **作者**：samzhu

---

## 1. 產品概述

### 1.1 產品名稱

**DocMCP Server** - Self-hosted Documentation MCP Server for AI Coding Assistants

### 1.2 產品定位

DocMCP Server 是一個**自託管**的 Model Context Protocol (MCP) 伺服器，專門為 AI 程式碼助手提供即時、版本正確的技術文件。不同於 Context7 等 SaaS 服務，DocMCP Server 讓開發團隊能夠在自己的基礎設施上運行，支援私有文件和自定義配置。

### 1.3 目標用戶

| 用戶類型 | 使用場景 |
|----------|----------|
| **個人開發者** | 為自己的 AI 助手提供私有專案文件 |
| **開發團隊** | 團隊共享的內部文件、架構指南、編碼規範 |
| **企業** | 企業私有文件、合規要求、安全敏感內容 |

### 1.4 核心價值主張

```
┌─────────────────────────────────────────────────────────────┐
│                    為什麼選擇 DocMCP Server？                 │
├─────────────────────────────────────────────────────────────┤
│  🏠 自託管        - 完全掌控數據，無需依賴外部服務              │
│  🔒 隱私安全      - 私有文件不會外洩到第三方服務                │
│  🎯 版本感知      - 自動提供版本匹配的文件內容                  │
│  🔍 智慧搜尋      - 全文搜尋 + 語意搜尋混合模式                 │
│  🧩 可擴展        - 模組化設計，易於擴展新的文件來源            │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 技術堆疊

### 2.1 已確定的技術選型

基於 `build.gradle` 的配置：

| 組件 | 技術 | 版本 |
|------|------|------|
| **語言** | Java | 25 |
| **框架** | Spring Boot | 4.0.1 |
| **AI 框架** | Spring AI | 2.0.0-M1 |
| **MCP 傳輸** | WebMVC (Stateless) | - |
| **向量儲存** | PostgreSQL + pgvector | - |
| **模板引擎** | Thymeleaf | - |
| **建構工具** | Gradle | - |
| **開發環境** | Docker Compose | - |
| **測試框架** | Testcontainers | - |

### 2.2 Gradle 依賴結構

```groovy
dependencies {
    // ===== Web & UI（網頁與使用者介面）=====
    // Thymeleaf 模板引擎，用於渲染管理介面的 HTML 頁面
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    // Spring MVC，提供 RESTful API 和 Web 端點
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // Spring Data JDBC，用於資料庫連線
    implementation 'org.springframework.boot:spring-boot-starter-data-jdbc'
    // Jakarta Validation，用於輸入驗證
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    // Spring Security，用於 API Key 認證
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // ===== MCP Server（模型上下文協議伺服器）=====
    // 使用 WebMVC 傳輸層的 MCP Server，支援 Streamable-HTTP 協議
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'

    // ===== Vector Store（向量儲存）=====
    // pgvector 向量儲存（使用 starter 自動配置）
    implementation 'org.springframework.ai:spring-ai-starter-vector-store-pgvector'

    // ===== Embedding Model（嵌入模型）=====
    // Google GenAI 嵌入模型（使用 starter 自動配置）
    // 使用 Gemini gemini-embedding-001，支援 768/1536/3072 維度
    implementation 'org.springframework.ai:spring-ai-starter-model-google-genai-embedding'

    // ===== 文件解析（Document Parsing）=====
    // HTML 解析（擷取 DOM 元素）
    implementation 'org.jsoup:jsoup:1.22.1'
    // 動態網頁渲染（處理 JavaScript 產生的內容）
    implementation 'org.htmlunit:htmlunit:4.21.0'
    // Markdown 解析與 HTML 轉 Markdown
    implementation 'com.vladsch.flexmark:flexmark:0.64.8'
    implementation 'com.vladsch.flexmark:flexmark-html2md-converter:0.64.8'
    // AsciiDoc 解析
    implementation 'org.asciidoctor:asciidoctorj:3.0.1'
    // 壓縮檔解壓縮（用於 GitHub tarball）
    implementation 'org.apache.commons:commons-compress:1.28.0'

    // ===== Development（開發環境）=====
    // Docker Compose 整合，啟動時自動拉起 compose.yaml 中定義的服務
    developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
    // 熱重載支援
    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    // ===== Testing（測試）=====
    // Spring Boot 測試框架基礎
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // Spring Security 測試支援
    testImplementation 'org.springframework.security:spring-security-test'
    // Testcontainers 整合，用於整合測試時自動啟動容器
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    // Spring AI 的 Testcontainers 支援
    testImplementation 'org.springframework.ai:spring-ai-spring-boot-testcontainers'
    // JUnit 5 與 Testcontainers 整合
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    // PostgreSQL Testcontainer，測試時自動啟動 PostgreSQL + pgvector
    testImplementation 'org.testcontainers:testcontainers-postgresql'
    // MockWebServer，用於模擬 HTTP 服務
    testImplementation 'com.squareup.okhttp3:mockwebserver:5.3.2'
}
```

> **設計決策：使用 Starter 版本**
> 專案目前使用 Spring AI 的 starter 版本（`spring-ai-starter-*`），原因：
> - **簡化配置**：自動配置減少樣板程式碼
> - **版本相容**：BOM 管理確保所有 Spring AI 元件版本一致
> - **快速開發**：適合 MVP 階段快速迭代
>
> 如需更細緻的控制（如自訂 VectorStore 表結構），可改用非 starter 版本並手動配置 Bean。

### 2.3 系統架構

系統採用經典的**分層架構（Layered Architecture）**，各層職責分明：

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DocMCP Server                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │              Presentation Layer（展示層）                     │    │
│  │  ┌───────────────────┐  ┌───────────────────────────────┐   │    │
│  │  │   MCP Endpoint    │  │    Web UI (Thymeleaf)         │   │    │
│  │  │  /mcp (HTTP)      │  │    - Dashboard（儀表板）       │   │    │
│  │  │  - Tools（工具）   │  │    - Library Management       │   │    │
│  │  │  - Resources      │  │      （函式庫管理）            │   │    │
│  │  │  - Prompts        │  │    - Search Interface         │   │    │
│  │  │                   │  │      （搜尋介面）              │   │    │
│  │  └───────────────────┘  └───────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                  │                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                Service Layer（服務層）                        │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │    │
│  │  │  Library    │  │   Search    │  │    Ingestion        │  │    │
│  │  │  Service    │  │   Service   │  │    Service          │  │    │
│  │  │ （函式庫）   │  │  （搜尋）    │  │   （文件攝取）       │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │    │
│  │  │  Document   │  │   Version   │  │    Embedding        │  │    │
│  │  │  Service    │  │   Service   │  │    Service          │  │    │
│  │  │  （文件）    │  │  （版本）    │  │   （向量嵌入）       │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                  │                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │               Repository Layer（資料存取層）                   │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │    │
│  │  │  Library    │  │   Document  │  │    Document         │  │    │
│  │  │  Repository │  │   Repository│  │    Chunk Repository │  │    │
│  │  │ （函式庫）   │  │  （文件）    │  │   （文件區塊）       │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                  │                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                Storage Layer（儲存層）                        │    │
│  │  ┌─────────────────────────────────────────────────────┐    │    │
│  │  │         PostgreSQL + pgvector                        │    │    │
│  │  │  - Relational Data（關聯式資料）                      │    │    │
│  │  │  - Full-text Search（全文搜尋：tsvector + GIN）       │    │    │
│  │  │  - Vector Search（向量搜尋：pgvector + HNSW）         │    │    │
│  │  └─────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

                    External Integrations（外部整合）
┌─────────────────────────────────────────────────────────────────────┐
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │
│  │   GitHub    │  │   Local     │  │    Embedding Provider       │  │
│  │   API       │  │   Files     │  │    （嵌入模型提供者）         │  │
│  │ （文件來源） │  │ （本地文件） │  │    Google GenAI             │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

**各層職責說明：**

| 層級 | 職責 | 主要元件 |
|------|------|----------|
| **展示層** | 處理外部請求，提供 MCP 端點和 Web 管理介面 | MCP Endpoint、Thymeleaf 頁面 |
| **服務層** | 實作業務邏輯，協調各種操作 | Library/Search/Document/Embedding Service |
| **資料存取層** | 封裝資料庫操作，提供統一的資料存取介面 | Spring Data JDBC Repository |
| **儲存層** | 實際的資料儲存，包含關聯式資料和向量資料 | PostgreSQL + pgvector |

### 2.4 Virtual Threads 架構

使用 Java 21 起正式支援的 Virtual Threads (JEP 444) 實現高併發處理：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Virtual Threads Executors                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │
│  │  mcp-executor   │  │  sync-executor  │  │ search-executor │      │
│  │  處理 MCP 請求   │  │  文件同步任務   │  │  搜尋查詢處理   │      │
│  │  Max: 200       │  │  Max: 50        │  │  Max: 100       │      │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘      │
│                                                                      │
│  ┌─────────────────┐  ┌─────────────────┐                           │
│  │ embed-executor  │  │ scheduled-exec  │                           │
│  │  Embedding 生成  │  │  排程任務       │                           │
│  │  Max: 20        │  │  Max: 10        │                           │
│  └─────────────────┘  └─────────────────┘                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**配置範例：**

```java
@Configuration
public class ExecutorConfig {

    @Bean("mcpExecutor")
    public ExecutorService mcpExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean("syncExecutor")
    public ExecutorService syncExecutor() {
        return Executors.newFixedThreadPool(50, Thread.ofVirtual().factory());
    }

    @Bean("searchExecutor")
    public ExecutorService searchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

**優勢：**
- 每個 MCP 請求使用獨立 Virtual Thread
- 高併發下記憶體使用效率高
- I/O 密集任務（搜尋、同步）效能提升

---

## 3. 功能需求

### 3.1 MCP 工具 (Tools)

DocMCP Server 提供以下 MCP 工具，設計遵循**分層式漸進查詢**模式。

> **實作狀態**：✅ 全部 12 個工具已實作完成

#### Level 1: Discovery (發現層)

| 工具 | 說明 | 優先級 | 狀態 |
|------|------|--------|------|
| `list_libraries` | 列出所有可用的文件庫 | P0 | ✅ 已完成 |
| `resolve_library` | 解析函式庫名稱為正規化 ID | P0 | ✅ 已完成 |

#### Level 2: Search (搜尋層)

| 工具 | 說明 | 優先級 | 狀態 |
|------|------|--------|------|
| `search_docs` | 跨文件庫搜尋文件內容（混合搜尋） | P0 | ✅ 已完成 |
| `semantic_search` | 純語意搜尋，適合概念性查詢 | P1 | ✅ 已完成 |
| `get_api_reference` | 取得特定 API 參考文件 | P1 | ✅ 已完成 |

#### Level 3: Retrieve (獲取層)

| 工具 | 說明 | 優先級 | 狀態 |
|------|------|--------|------|
| `get_doc_content` | 取得特定文件的完整內容 | P0 | ✅ 已完成 |
| `get_doc_toc` | 取得文件目錄結構 | P1 | ✅ 已完成 |
| `get_related_docs` | 取得相關文件推薦 | P1 | ✅ 已完成 |
| `get_code_examples` | 取得程式碼範例 | P1 | ✅ 已完成 |
| `get_migration_guide` | 取得版本遷移指南 | P2 | ✅ 已完成 |

#### Level 4: Management (管理層)

| 工具 | 說明 | 優先級 | 狀態 |
|------|------|--------|------|
| `list_versions` | 列出函式庫所有版本 | P1 | ✅ 已完成 |
| `get_sync_status` | 取得同步狀態與歷史 | P1 | ✅ 已完成 |

### 3.2 工具詳細規格

#### 3.2.1 list_libraries

```java
@McpTool(
    name = "list_libraries",
    description = "List all available documentation libraries. " +
                  "Use this to discover what libraries are indexed.")
public ListLibrariesResult listLibraries(
    @McpToolParam(description = "Filter by category (e.g., 'java', 'frontend')",
                  required = false)
    String category,

    @McpToolParam(description = "Include version information",
                  required = false)
    Boolean includeVersions
);
```

**回傳範例：**
```json
{
  "libraries": [
    {
      "id": "spring-boot",
      "name": "Spring Boot",
      "description": "Spring Boot framework documentation",
      "category": "java",
      "latestVersion": "3.5.0",
      "availableVersions": ["3.5.0", "3.4.0", "3.3.0"]
    }
  ],
  "total": 1
}
```

#### 3.2.2 resolve_library

```java
@McpTool(
    name = "resolve_library",
    description = "Resolve a library name to its canonical ID and version. " +
                  "Use this before querying documentation.")
public ResolveLibraryResult resolveLibrary(
    @McpToolParam(description = "Library name (e.g., 'spring-boot', 'react')",
                  required = true)
    String name,

    @McpToolParam(description = "Version hint (e.g., '3.5', 'latest', 'lts')",
                  required = false)
    String version
);
```

**回傳範例：**
```json
{
  "id": "spring-boot",
  "resolvedVersion": "3.5.0",
  "name": "Spring Boot",
  "description": "Spring Boot framework documentation",
  "documentationUrl": "https://docs.spring.io/spring-boot/",
  "sourceType": "GITHUB",
  "sourceUrl": "https://github.com/spring-projects/spring-boot"
}
```

#### 3.2.3 search_docs

```java
@McpTool(
    name = "search_docs",
    description = "Search documentation across libraries using hybrid search. " +
                  "Combines full-text and semantic search for best results.")
public SearchDocsResult searchDocs(
    @McpToolParam(description = "Search query",
                  required = true)
    String query,

    @McpToolParam(description = "Library ID to search within (optional)",
                  required = false)
    String libraryId,

    @McpToolParam(description = "Specific version (optional)",
                  required = false)
    String version,

    @McpToolParam(description = "Maximum results (default: 5, max: 20)",
                  required = false)
    Integer limit,

    @McpToolParam(description = "Include code snippets in results",
                  required = false)
    Boolean includeCode
);
```

**回傳範例：**
```json
{
  "results": [
    {
      "title": "Getting Started with Spring Boot",
      "uri": "spring-boot:3.5.0:/docs/getting-started.md",
      "snippet": "Spring Boot makes it easy to create stand-alone...",
      "score": 0.95,
      "library": "spring-boot",
      "version": "3.5.0",
      "codeSnippets": [
        {
          "language": "java",
          "code": "@SpringBootApplication\npublic class Application {...}"
        }
      ]
    }
  ],
  "total": 1,
  "searchType": "HYBRID"
}
```

#### 3.2.4 get_doc_content

```java
@McpTool(
    name = "get_doc_content",
    description = "Get the full content of a specific documentation page.")
public GetDocContentResult getDocContent(
    @McpToolParam(description = "Document URI (from search results)",
                  required = true)
    String docUri,

    @McpToolParam(description = "Specific section or heading to focus on",
                  required = false)
    String section,

    @McpToolParam(description = "Maximum tokens in response (default: 8000)",
                  required = false)
    Integer maxTokens
);
```

**回傳範例：**
```json
{
  "title": "Getting Started with Spring Boot",
  "uri": "spring-boot:3.5.0:/docs/getting-started.md",
  "content": "# Getting Started\n\nSpring Boot makes it easy to...",
  "library": "spring-boot",
  "version": "3.5.0",
  "lastUpdated": "2026-01-15T10:30:00Z",
  "sections": ["Introduction", "Prerequisites", "Quick Start"],
  "relatedDocs": [
    {"title": "Spring Boot Reference", "uri": "spring-boot:3.5.0:/docs/reference.md"}
  ]
}
```

#### 3.2.5 get_code_examples

```java
@McpTool(
    name = "get_code_examples",
    description = "Get code examples for a specific topic or API.")
public GetCodeExamplesResult getCodeExamples(
    @McpToolParam(description = "Library ID",
                  required = true)
    String libraryId,

    @McpToolParam(description = "Topic or API name (e.g., '@RestController', 'hooks')",
                  required = true)
    String topic,

    @McpToolParam(description = "Programming language filter",
                  required = false)
    String language,

    @McpToolParam(description = "Maximum examples (default: 3)",
                  required = false)
    Integer limit
);
```

#### 3.2.6 semantic_search

```java
@McpTool(
    name = "semantic_search",
    description = "Pure semantic search for conceptual queries. " +
                  "Use this when searching for concepts rather than exact terms.")
public SemanticSearchResult semanticSearch(
    @McpToolParam(description = "Search query (natural language)",
                  required = true)
    String query,

    @McpToolParam(description = "Library ID to search within",
                  required = false)
    String libraryId,

    @McpToolParam(description = "Minimum similarity score (0.0-1.0, default: 0.7)",
                  required = false)
    Double minSimilarity,

    @McpToolParam(description = "Maximum results (default: 5)",
                  required = false)
    Integer limit
);
```

#### 3.2.7 get_doc_toc

```java
@McpTool(
    name = "get_doc_toc",
    description = "Get the table of contents for a documentation library. " +
                  "Useful for understanding the documentation structure.")
public GetDocTocResult getDocToc(
    @McpToolParam(description = "Library ID",
                  required = true)
    String libraryId,

    @McpToolParam(description = "Specific version (optional, defaults to latest)",
                  required = false)
    String version,

    @McpToolParam(description = "Maximum depth of TOC (default: 3)",
                  required = false)
    Integer maxDepth
);
```

**回傳範例：**
```json
{
  "library": "spring-boot",
  "version": "3.5.0",
  "toc": [
    {
      "title": "Getting Started",
      "uri": "spring-boot:3.5.0:/docs/getting-started.md",
      "children": [
        {"title": "Installation", "uri": "..."},
        {"title": "Quick Start", "uri": "..."}
      ]
    }
  ]
}
```

#### 3.2.8 get_related_docs

```java
@McpTool(
    name = "get_related_docs",
    description = "Get related documents based on content similarity.")
public GetRelatedDocsResult getRelatedDocs(
    @McpToolParam(description = "Document URI",
                  required = true)
    String docUri,

    @McpToolParam(description = "Maximum related docs (default: 5)",
                  required = false)
    Integer limit
);
```

#### 3.2.9 list_versions

```java
@McpTool(
    name = "list_versions",
    description = "List all available versions for a library with status info.")
public ListVersionsResult listVersions(
    @McpToolParam(description = "Library ID",
                  required = true)
    String libraryId,

    @McpToolParam(description = "Include deprecated versions",
                  required = false)
    Boolean includeDeprecated
);
```

**回傳範例：**
```json
{
  "library": "spring-boot",
  "versions": [
    {"version": "3.5.0", "status": "active", "isLatest": true, "isLts": false},
    {"version": "3.4.0", "status": "active", "isLatest": false, "isLts": true},
    {"version": "3.3.0", "status": "deprecated", "isLatest": false, "isLts": false}
  ]
}
```

#### 3.2.10 get_sync_status

```java
@McpTool(
    name = "get_sync_status",
    description = "Get synchronization status and history for a library.")
public GetSyncStatusResult getSyncStatus(
    @McpToolParam(description = "Library ID",
                  required = true)
    String libraryId
);
```

**回傳範例：**
```json
{
  "library": "spring-boot",
  "lastSync": "2026-01-19T02:00:00Z",
  "status": "SUCCESS",
  "documentsIndexed": 1523,
  "chunksCreated": 8945,
  "syncDuration": "PT5M32S",
  "recentHistory": [
    {"timestamp": "2026-01-19T02:00:00Z", "status": "SUCCESS", "added": 15, "updated": 3},
    {"timestamp": "2026-01-18T02:00:00Z", "status": "SUCCESS", "added": 0, "updated": 8}
  ]
}
```

### 3.3 MCP 資源 (Resources)

> **實作狀態**：✅ 全部 2 個 Resource Provider 已實作完成

| 資源 URI 模板 | 說明 | 優先級 | 狀態 |
|---------------|------|--------|------|
| `docs://{libraryName}/{version}/{path}` | 文件內容資源 | P0 | ✅ 已完成 |
| `docs://{libraryName}/latest/{path}` | 最新版本文件內容 | P0 | ✅ 已完成 |
| `library://{libraryName}` | 函式庫元資料 | P1 | ✅ 已完成 |
| `library://{libraryName}/{version}` | 特定版本元資料 | P1 | ✅ 已完成 |

### 3.4 MCP Prompts (提示模板)

> **實作狀態**：✅ 全部 5 個 Prompts 已實作完成

MCP Prompts 提供預定義的提示模板，讓使用者在 AI 助手中可以快速選用（如斜線命令 `/docmcp_explain_library`）。

| Prompt 名稱 | 說明 | 參數 | 狀態 |
|-------------|------|------|------|
| `docmcp_explain_library` | 解釋函式庫核心概念和快速入門 | `library`(必要), `version`(選填) | ✅ 已完成 |
| `docmcp_search_usage` | 搜尋特定功能的使用方式 | `library`(必要), `topic`(必要), `version`(選填) | ✅ 已完成 |
| `docmcp_compare_versions` | 比較兩個版本的差異 | `library`(必要), `from_version`(必要), `to_version`(選填) | ✅ 已完成 |
| `docmcp_generate_example` | 生成程式碼範例 | `library`(必要), `api_or_feature`(必要), `language`(選填), `version`(選填) | ✅ 已完成 |
| `docmcp_troubleshoot` | 根據錯誤訊息提供解決方案 | `library`(必要), `error_or_issue`(必要), `version`(選填) | ✅ 已完成 |

**設計原則**：
- 每個 Prompt 引導 AI 使用現有的 Tools（`search_docs`、`get_code_examples` 等）
- 使用 `docmcp_` 前綴，符合 MCP 最佳實踐的 Service-Prefixed Naming
- 可選參數為 null 時使用預設值（如「最新版本」）

### 3.6 Web UI 功能

使用 Thymeleaf 建立管理介面，採用 Apple Liquid Glass 設計風格。

> **實作狀態**：✅ 核心頁面已完成

**Sidebar 主要入口**：

| 頁面 | 功能 | 優先級 | 狀態 |
|------|------|--------|------|
| **Dashboard** | 系統概覽、統計資訊 | P1 | ✅ 已完成 |
| **Libraries** | 函式庫管理 (列表/詳情/新增/編輯) | P0 | ✅ 已完成 |
| **Search** | 文件搜尋介面 | P1 | ✅ 已完成 |
| **Sync Status** | 同步狀態與日誌 (列表/詳情) | P1 | ✅ 已完成 |
| **API Keys** | API 金鑰管理 | P1 | ✅ 已完成 |
| **Setup** | 初始設定（MCP 整合指南） | P2 | ✅ 已完成 |
| **Settings** | 系統設定 | P2 | ✅ 已完成 |

**子頁面**（從主要入口進入）：

| 頁面 | 進入路徑 | 功能 |
|------|----------|------|
| **Documents** | Libraries → Library Detail → Version → Browse Docs | 文件瀏覽 (列表/詳情) |

---

## 4. 資料模型

### 4.1 核心實體

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│    Library      │       │ LibraryVersion  │       │    Document     │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (UUID)       │──┐    │ id (UUID)       │──┐    │ id (UUID)       │
│ name            │  │    │ library_id (FK) │  │    │ version_id (FK) │
│ displayName     │  └───>│ version         │  └───>│ uri             │
│ description     │       │ status          │       │ title           │
│ category        │       │ isLatest        │       │ content         │
│ sourceType      │       │ isLts           │       │ contentType     │
│ sourceUrl       │       │ releaseDate     │       │ metadata (JSON) │
│ syncConfig      │       │ syncedAt        │       │ createdAt       │
│ createdAt       │       └─────────────────┘       │ updatedAt       │
│ updatedAt       │              │                  └─────────────────┘
└─────────────────┘              │                           │
        │                        │                    ┌──────┴──────┐
        │                        ▼                    │             │
        │              ┌─────────────────┐            ▼             ▼
        │              │   SyncHistory   │  ┌─────────────────┐ ┌─────────────────┐
        │              ├─────────────────┤  │  DocumentChunk  │ │   CodeExample   │
        │              │ id (UUID)       │  ├─────────────────┤ ├─────────────────┤
        └─────────────>│ library_id (FK) │  │ id (UUID)       │ │ id (UUID)       │
                       │ version_id (FK) │  │ document_id(FK) │ │ document_id(FK) │
                       │ status          │  │ chunkIndex      │ │ title           │
                       │ started_at      │  │ content         │ │ language        │
                       │ completed_at    │  │ searchVector    │ │ code            │
                       │ docs_added      │  │ embedding       │ │ description     │
                       │ docs_updated    │  │ metadata (JSON) │ │ category        │
                       │ error_message   │  └─────────────────┘ │ tags (JSON)     │
                       └─────────────────┘                      └─────────────────┘
```

### 4.2 資料庫 Schema

使用 PostgreSQL 搭配 pgvector 擴充套件，同時支援關聯式資料和向量資料：

```sql
-- =====================================================
-- 啟用必要的 PostgreSQL 擴充套件
-- =====================================================
CREATE EXTENSION IF NOT EXISTS vector;      -- pgvector：向量儲存與相似度搜尋
CREATE EXTENSION IF NOT EXISTS hstore;      -- 鍵值對儲存（可選）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- UUID 生成函數

-- =====================================================
-- 函式庫表（Library）
-- 說明：儲存文件庫的基本資訊，例如 Spring Boot、React 等
-- =====================================================
CREATE TABLE libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- 主鍵，使用 UUID
    name VARCHAR(100) NOT NULL UNIQUE,               -- 唯一識別名稱（例：spring-boot）
    display_name VARCHAR(255) NOT NULL,              -- 顯示名稱（例：Spring Boot）
    description TEXT,                                 -- 函式庫描述
    category VARCHAR(50),                            -- 分類（例：java, frontend, devops）
    source_type VARCHAR(20) NOT NULL,                -- 來源類型：GITHUB, LOCAL, MANUAL
    source_url TEXT,                                  -- 來源 URL（例：GitHub 倉庫地址）
    sync_config JSONB DEFAULT '{}',                  -- 同步配置（JSON 格式，彈性擴展）
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- 版本表（LibraryVersion）
-- 說明：儲存函式庫的各個版本資訊
-- =====================================================
CREATE TABLE library_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,  -- 關聯到函式庫
    version VARCHAR(50) NOT NULL,                    -- 版本號（例：3.5.0）
    status VARCHAR(20) DEFAULT 'active',             -- 狀態：active（使用中）, deprecated（已棄用）, eol（終止支援）
    is_latest BOOLEAN DEFAULT FALSE,                 -- 是否為最新版本
    is_lts BOOLEAN DEFAULT FALSE,                    -- 是否為長期支援版本（Long Term Support）
    release_date DATE,                               -- 發布日期
    synced_at TIMESTAMP WITH TIME ZONE,              -- 最後同步時間
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(library_id, version)                      -- 同一函式庫的版本不可重複
);

-- =====================================================
-- 文件表（Document）
-- 說明：儲存文件的原始內容和元資料
-- =====================================================
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL REFERENCES library_versions(id) ON DELETE CASCADE,  -- 關聯到版本
    uri TEXT NOT NULL,                               -- 文件 URI（例：/docs/getting-started.md）
    title VARCHAR(500),                              -- 文件標題
    content TEXT,                                    -- 文件完整內容
    content_type VARCHAR(50) DEFAULT 'text/markdown', -- 內容類型（Markdown, HTML 等）
    metadata JSONB DEFAULT '{}',                     -- 元資料（標籤、作者等）
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(version_id, uri)                          -- 同一版本的 URI 不可重複
);

-- =====================================================
-- 文件區塊表（DocumentChunk）
-- 說明：將文件切割成小區塊，用於向量搜尋
-- 這是實現語意搜尋的核心資料表
-- =====================================================
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,  -- 關聯到文件
    chunk_index INTEGER NOT NULL,                    -- 區塊在文件中的順序（0, 1, 2...）
    content TEXT NOT NULL,                           -- 區塊內容（約 500-1000 字元）
    search_vector tsvector,                          -- 全文搜尋向量（PostgreSQL 內建）
    embedding vector(768),                           -- 語意向量（Gemini gemini-embedding-001，選擇 768 維度以節省資源）
    metadata JSONB DEFAULT '{}',                     -- 元資料（標題層級、所屬章節等）
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(document_id, chunk_index)                 -- 同一文件的區塊索引不可重複
);

-- =====================================================
-- 程式碼範例表（CodeExample）
-- 說明：從文件中擷取的程式碼範例
-- =====================================================
CREATE TABLE code_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    title VARCHAR(255),                              -- 範例標題
    language VARCHAR(50) NOT NULL,                   -- 程式語言（java, kotlin, python 等）
    code TEXT NOT NULL,                              -- 程式碼內容
    description TEXT,                                -- 範例說明
    category VARCHAR(50),                            -- 分類（basic, advanced, integration 等）
    tags JSONB DEFAULT '[]',                         -- 標籤陣列（例：["spring-boot", "configuration"]）
    source_line_start INTEGER,                       -- 原始文件中的起始行號
    source_line_end INTEGER,                         -- 原始文件中的結束行號
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- 同步歷史表（SyncHistory）
-- 說明：記錄每次文件同步的執行狀態和統計資訊
-- =====================================================
CREATE TABLE sync_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    version_id UUID REFERENCES library_versions(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL,                     -- 狀態：PENDING, RUNNING, SUCCESS, FAILED, PARTIAL
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,   -- 開始時間
    completed_at TIMESTAMP WITH TIME ZONE,           -- 完成時間
    documents_added INTEGER DEFAULT 0,               -- 新增文件數
    documents_updated INTEGER DEFAULT 0,             -- 更新文件數
    documents_deleted INTEGER DEFAULT 0,             -- 刪除文件數
    chunks_created INTEGER DEFAULT 0,                -- 建立的區塊數
    error_message TEXT,                              -- 錯誤訊息（如果失敗）
    error_details JSONB,                             -- 詳細錯誤資訊（堆疊追蹤等）
    metadata JSONB DEFAULT '{}'                      -- 額外統計資訊
);

-- =====================================================
-- 索引設計
-- 說明：建立適當的索引以提升查詢效能
-- =====================================================

-- 一般查詢索引（B-tree，PostgreSQL 預設）
CREATE INDEX idx_libraries_category ON libraries(category);                    -- 按分類查詢函式庫
CREATE INDEX idx_library_versions_library ON library_versions(library_id);    -- 按函式庫查詢版本
CREATE INDEX idx_library_versions_latest ON library_versions(is_latest)       -- 快速找到最新版本
    WHERE is_latest = TRUE;                                                    -- 部分索引，只索引 is_latest=true 的資料
CREATE INDEX idx_documents_version ON documents(version_id);                   -- 按版本查詢文件
CREATE INDEX idx_document_chunks_document ON document_chunks(document_id);    -- 按文件查詢區塊
CREATE INDEX idx_code_examples_document ON code_examples(document_id);        -- 按文件查詢程式碼範例
CREATE INDEX idx_code_examples_language ON code_examples(language);           -- 按程式語言篩選
CREATE INDEX idx_sync_history_library ON sync_history(library_id);            -- 按函式庫查詢同步歷史
CREATE INDEX idx_sync_history_status ON sync_history(status);                 -- 按狀態篩選

-- 全文搜尋索引（GIN，用於 tsvector）
-- GIN = Generalized Inverted Index，適合全文搜尋
CREATE INDEX idx_document_chunks_search ON document_chunks USING GIN(search_vector);

-- 向量搜尋索引 (IVFFlat for large datasets)
CREATE INDEX idx_document_chunks_embedding ON document_chunks
    USING ivfflat(embedding vector_cosine_ops) WITH (lists = 100);

-- 自動更新 search_vector 觸發器
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_search_vector
    BEFORE INSERT OR UPDATE ON document_chunks
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();
```

---

## 5. API 設計

### 5.1 MCP 端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/mcp` | POST | MCP JSON-RPC 請求 (Stateless) |

### 5.2 Web API (供 UI 使用)

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/libraries` | GET | 列出函式庫 |
| `/api/libraries` | POST | 新增函式庫 |
| `/api/libraries/{id}` | GET | 取得函式庫詳情 |
| `/api/libraries/{id}` | PUT | 更新函式庫 |
| `/api/libraries/{id}` | DELETE | 刪除函式庫 |
| `/api/libraries/{id}/sync` | POST | 觸發同步 |
| `/api/search` | GET | 搜尋文件 |

---

## 6. 配置設計

### 6.1 application.yaml

配置採用分層設計，基礎配置在 `application.yaml`，環境特定配置透過 profile 檔案覆蓋：

```yaml
# =============================================================================
# DocMCP Server - 基礎共用配置
# =============================================================================
spring:
  application:
    name: DocMCP Server

  threads:
    virtual:
      enabled: true  # Java 21+ Virtual Threads

  # ----- 資料庫配置 -----
  datasource:
    url: ${docmcp-db-url:jdbc:postgresql://localhost:5432/mydatabase}
    username: ${docmcp-db-username:myuser}
    password: ${docmcp-db-password:secret}

  sql:
    init:
      mode: always
      platform: postgresql

  # ----- Spring AI MCP Server 配置 -----
  ai:
    mcp:
      server:
        protocol: STATELESS          # 無狀態傳輸（適合雲端部署）
        name: DocMCP Server
        version: 0.1.0
        annotation-scanner:
          enabled: true              # 啟用 MCP 註解掃描

    # ----- Google GenAI Embedding 配置 -----
    google:
      genai:
        embedding:
          api-key: ${docmcp-google-api-key:}
          text:
            options:
              model: gemini-embedding-001
              dimensions: 768        # 向量維度

    # ----- PgVector 向量儲存配置 -----
    vectorstore:
      pgvector:
        dimensions: 768
        distance-type: COSINE_DISTANCE
        index-type: HNSW

# =============================================================================
# DocMCP Server 自訂配置
# =============================================================================
docmcp:
  # ----- 功能開關 -----
  features:
    web-ui: true            # Web UI 介面
    api-key-auth: false     # API Key 認證（生產環境建議開啟）
    semantic-search: true   # 語意搜尋
    code-examples: true     # 程式碼範例
    migration-guides: false # 遷移指南（P2 延後）
    sync-scheduling: false  # 同步排程
    concurrency-limit: true # 併發限制

  # ----- 搜尋配置 -----
  search:
    hybrid:
      alpha: 0.3            # 關鍵字權重（30% 關鍵字、70% 語意）
      min-similarity: 0.5   # 最低相似度閾值

  # ----- 同步排程配置 -----
  sync:
    cron: "0 0 2 * * *"     # 每天凌晨 2 點

  # ----- GitHub 取得配置 -----
  github:
    fetch:
      connect-timeout-ms: 10000
      read-timeout-ms: 30000
      archive:
        enabled: true
        priority: 1          # 優先使用 tarball 下載
      git-tree:
        enabled: true
        priority: 2
      contents-api:
        enabled: true
        priority: 3

  # ----- 安全配置 -----
  security:
    api-key:
      enabled: false
      header-name: Authorization
      prefix: "Bearer "
      allow-anonymous: true
    concurrency:
      default-limit: 10
      anonymous-limit: 5
```

### 6.2 配置檔案結構

| 檔案 | 說明 | 用途 |
|------|------|------|
| `src/main/resources/application.yaml` | 基礎配置 | 包進 Docker Image |
| `src/main/resources/application-local.yaml` | 本地開發配置 | 覆蓋資料庫連線等 |
| `config/application-dev.yaml` | 開發環境配置 | DEBUG 日誌等 |
| `config/application-secrets.properties` | 敏感資訊 | gitignored |

### 6.3 環境變數

敏感資訊透過統一命名的環境變數注入：

| 環境變數 | 說明 | 必要性 |
|----------|------|--------|
| `docmcp-google-api-key` | Google GenAI API Key | 必要 |
| `docmcp-db-url` | 資料庫連線 URL | 必要 |
| `docmcp-db-username` | 資料庫使用者名稱 | 必要 |
| `docmcp-db-password` | 資料庫密碼 | 必要 |

### 6.4 功能開關模式

採用 Feature Toggle 模式，讓功能可以獨立啟用/停用：

```java
@Component
@ConfigurationProperties(prefix = "docmcp.features")
public class FeatureFlags {

    private FeatureConfig webUi = new FeatureConfig(true);
    private FeatureConfig apiKeyAuth = new FeatureConfig(true);
    private FeatureConfig semanticSearch = new FeatureConfig(true);
    private FeatureConfig codeExamples = new FeatureConfig(true);
    private FeatureConfig migrationGuides = new FeatureConfig(false);
    private FeatureConfig syncScheduling = new FeatureConfig(true);

    // getters, setters

    public record FeatureConfig(boolean enabled) {}
}
```

**使用範例：**

```java
@McpTool(name = "get_migration_guide")
@ConditionalOnProperty(name = "docmcp.features.migration-guides.enabled", havingValue = "true")
public class MigrationGuideTool {
    // 只有在 feature 啟用時才註冊此工具
}
```

### 6.5 混合搜尋演算法

採用 spring-documentation-mcp-server 的混合搜尋公式：

```
final_score = (1 - α) × semantic_score + α × keyword_score
```

**參數說明：**

| 參數 | 預設值 | 說明 |
|------|--------|------|
| `α` (alpha) | 0.3 | 關鍵字權重，較低值偏重語意搜尋 |
| `min-similarity` | 0.7 | 過濾低於此分數的結果 |

**搜尋流程：**

```
1. 使用者輸入查詢 "Spring Boot configuration"
     │
     ▼
2. 並行執行兩種搜尋
   ┌─────────────────────┬─────────────────────┐
   │   tsvector 全文搜尋  │   pgvector 語意搜尋  │
   │   ts_rank()         │   cosine similarity │
   └─────────────────────┴─────────────────────┘
     │                         │
     ▼                         ▼
3. 分數正規化 (0.0 - 1.0)
     │
     ▼
4. 套用公式計算 final_score
     │
     ▼
5. 過濾 < min-similarity 的結果
     │
     ▼
6. 依 final_score 排序並回傳

---

## 7. 開發計畫

### 7.1 Phase 1: 基礎架構 (MVP)

**目標**：建立可運行的 MCP Server 骨架

| 任務 | 說明 | 預估 |
|------|------|------|
| 專案結構設計 | 建立 package 結構、基礎配置 | - |
| 資料庫 Schema | 實作上述 Schema，配置 Flyway migration | - |
| 基礎 Repository | Library, Document, DocumentChunk Repository | - |
| MCP 工具骨架 | 實作 `list_libraries`, `resolve_library` | - |
| Docker Compose | 配置開發環境 (PostgreSQL + pgvector) | - |

**驗收標準**：
- [ ] 應用程式可啟動
- [ ] MCP 端點可回應 `tools/list`
- [ ] 資料庫連接正常

### 7.2 Phase 2: 核心搜尋功能

**目標**：實作搜尋功能

| 任務 | 說明 | 預估 |
|------|------|------|
| 全文搜尋 | 使用 PostgreSQL tsvector | - |
| Embedding 生成 | 整合 OpenAI/Ollama embedding | - |
| 向量搜尋 | 使用 pgvector 實作語意搜尋 | - |
| 混合搜尋 | 整合全文與向量搜尋 | - |
| MCP 工具 | 實作 `search_docs`, `get_doc_content` | - |

**驗收標準**：
- [ ] 可透過 MCP 工具搜尋文件
- [ ] 搜尋結果包含相關性分數
- [ ] 支援版本過濾

### 7.3 Phase 3: 文件攝取

**目標**：自動同步外部文件

| 任務 | 說明 | 預估 |
|------|------|------|
| GitHub 整合 | 從 GitHub repo 抓取文件 | - |
| 文件解析 | Markdown/HTML 解析與清理 | - |
| 區塊化處理 | 智慧文件切割 | - |
| 排程同步 | 定時同步任務 | - |
| 增量更新 | 只更新變更的文件 | - |

**驗收標準**：
- [ ] 可新增 GitHub repo 作為文件來源
- [ ] 自動解析並索引文件
- [ ] 排程同步正常運作

### 7.4 Phase 4: Web 管理介面

**目標**：提供視覺化管理

| 任務 | 說明 | 預估 |
|------|------|------|
| Dashboard | 系統概覽頁面 | - |
| Library 管理 | CRUD 介面 | - |
| 搜尋介面 | Web 搜尋功能 | - |
| 同步監控 | 同步狀態與日誌 | - |

**驗收標準**：
- [ ] 可透過 Web 介面管理函式庫
- [ ] 可查看同步狀態
- [ ] 可手動觸發同步

### 7.5 Phase 5: 進階功能

**目標**：增強功能與穩定性

| 任務 | 說明 | 預估 |
|------|------|------|
| 程式碼範例 | `get_code_examples` 工具 | - |
| 本地文件 | 支援本地目錄作為來源 | - |
| API 認證 | API Key 認證機制 | - |
| 監控 | Actuator + Metrics | - |
| 效能優化 | 快取、查詢優化 | - |

---

## 8. 非功能需求

### 8.1 效能

| 指標 | 目標 |
|------|------|
| 搜尋延遲 (P95) | < 500ms |
| MCP 工具回應 (P95) | < 1s |
| 同時連線數 | 100+ |
| 索引文件數 | 100,000+ |

### 8.2 可靠性

| 指標 | 目標 |
|------|------|
| 可用性 | 99.9% |
| 資料持久性 | 資料庫備份 |
| 錯誤恢復 | 優雅降級 |

### 8.3 安全性

#### 8.3.1 API Key 認證

**認證機制設計：**

```
┌─────────────────────────────────────────────────────────────────────┐
│                      API Key Authentication Flow                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client                          Server                              │
│    │                               │                                 │
│    │  POST /mcp                    │                                 │
│    │  Authorization: Bearer <key>  │                                 │
│    │──────────────────────────────>│                                 │
│    │                               │  1. Extract API Key             │
│    │                               │  2. BCrypt verify against DB    │
│    │                               │  3. Check key status (active)   │
│    │                               │  4. Update last_used_at         │
│    │                               │                                 │
│    │         200 OK                │                                 │
│    │<──────────────────────────────│                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**API Key 資料模型：**

```sql
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,          -- 識別名稱 (e.g., "Claude Desktop - Work")
    key_hash VARCHAR(255) NOT NULL,      -- BCrypt 雜湊後的 API Key
    key_prefix VARCHAR(8) NOT NULL,      -- Key 前綴用於識別 (e.g., "dmcp_a1b2")
    status VARCHAR(20) DEFAULT 'active', -- active, revoked, expired
    permissions JSONB DEFAULT '[]',      -- 權限設定 (保留未來擴展)
    rate_limit INTEGER DEFAULT 1000,     -- 每小時請求上限
    expires_at TIMESTAMP WITH TIME ZONE, -- 過期時間 (可選)
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    UNIQUE(key_prefix)
);
```

**API Key 格式：**
```
dmcp_[random-32-chars]
例：dmcp_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

**認證配置：**

```yaml
docmcp:
  security:
    api-key:
      enabled: true                    # 是否啟用 API Key 認證
      header-name: Authorization       # Header 名稱
      prefix: "Bearer "                # Key 前綴
      allow-anonymous: false           # 是否允許匿名存取
      rate-limit:
        enabled: true
        requests-per-hour: 1000
```

#### 8.3.2 其他安全措施

- HTTPS 支援（由外部處理，如 Cloud Run、Nginx 等反向代理）
- SQL Injection 防護 (使用 Spring Data JDBC 參數化查詢)
- 輸入驗證 (Jakarta Validation)
- Rate Limiting (基於 API Key)
- 敏感資料不記錄於日誌

### 8.4 可觀測性

- Structured Logging (JSON)
- Spring Boot Actuator
- Prometheus Metrics (可選)

---

## 9. 風險與緩解

| 風險 | 影響 | 緩解措施 |
|------|------|----------|
| Embedding API 成本 | 高 | 支援本地 Ollama 作為替代 |
| 大型文件處理 | 記憶體不足 | 串流處理、分頁載入 |
| GitHub Rate Limit | 同步失敗 | 實作 rate limiting、快取 |
| 向量索引效能 | 搜尋變慢 | 使用 IVFFlat 索引、定期重建 |

---

## 10. 成功指標

### 10.1 功能指標

- [ ] 可索引 10+ 開源專案文件
- [ ] 搜尋準確率 > 80%
- [ ] MCP 工具可被 Claude Desktop/Cursor 正常使用

### 10.2 技術指標

- [ ] 測試覆蓋率 > 70%
- [ ] 無嚴重安全漏洞
- [ ] 可在 2GB RAM 環境運行

---

## 11. 附錄

### 11.1 參考資料

| 資源 | 連結 |
|------|------|
| MCP 規格 | https://modelcontextprotocol.io/specification/2025-11-25 |
| Spring AI MCP 文件 | https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html |
| Spring AI pgvector | https://docs.spring.io/spring-ai/reference/2.0/api/vectordbs/pgvector.html |
| Spring AI Google GenAI Embedding | https://docs.spring.io/spring-ai/reference/2.0/api/embeddings/google-genai-embeddings-text.html |
| pgvector 文件 | https://github.com/pgvector/pgvector |
| Context7 | https://github.com/upstash/context7 |
| spring-documentation-mcp-server | https://github.com/andrlange/spring-documentation-mcp-server |

### 11.2 術語表

| 術語 | 定義 |
|------|------|
| **MCP** | Model Context Protocol，LLM 與外部工具的標準協議 |
| **Tool** | MCP 中由 AI 模型調用的功能 |
| **Resource** | MCP 中提供資料的 URI 資源 |
| **Embedding** | 文字的向量表示，用於語意搜尋 |
| **Chunk** | 文件切割後的片段，適合向量化 |
| **pgvector** | PostgreSQL 的向量搜尋擴展 |

---

## 變更記錄

| 版本 | 日期 | 變更內容 | 作者 |
|------|------|----------|------|
| 1.0.0 | 2026-01-19 | 初始版本 | samzhu |
| 1.1.0 | 2026-01-19 | 整合 spring-documentation-mcp-server 優秀設計 | samzhu |
| 1.2.0 | 2026-01-19 | 改用 Gemini text-embedding-004，非 starter 手動配置 | samzhu |
| 1.3.0 | 2026-01-19 | 新增繁體中文詳細註解說明 | samzhu |
| 1.4.0 | 2026-01-19 | 更新為 gemini-embedding-001（text-embedding-004 已於 2026/1/14 停用），使用 768 維度以符合 2GB RAM 限制，修正 JEP 444 描述 | samzhu |
| 1.5.0 | 2026-01-19 | MCP 協議改為 STATELESS（適合雲端部署、無狀態水平擴展），修正 spring.ai.mcp.server.protocol 設定 | samzhu |
| 1.6.0 | 2026-01-20 | 明確 HTTPS 由外部處理（Cloud Run 等反向代理），應用層不需處理 | samzhu |
| 1.7.0 | 2026-01-22 | 同步專案現狀：更新 Gradle 依賴（改用 starter 版本）、標記所有 MCP Tools/Resources 為已完成、新增 MCP Prompts 章節（5 個 Prompts）、更新 Web UI 頁面清單 | Claude |
