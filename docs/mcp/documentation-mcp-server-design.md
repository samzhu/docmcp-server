# 現代化 Documentation MCP Server 設計指南

> 最後更新：2026-01-19
> 難度：進階
> 前置知識：MCP 規格、Spring AI MCP 開發、軟體架構設計

## 目錄

1. [簡介](#簡介)
2. [參考專案分析](#參考專案分析)
3. [架構設計](#架構設計)
4. [核心功能模組](#核心功能模組)
5. [MCP 工具設計](#mcp-工具設計)
6. [資料儲存方案](#資料儲存方案)
7. [最佳實踐](#最佳實踐)
8. [實作建議](#實作建議)
9. [參考來源](#參考來源)

---

## 簡介

### 什麼是 Documentation MCP Server？

Documentation MCP Server 是一種專門用於提供技術文件給 LLM 的 MCP 服務。它讓 AI 助手能夠存取最新、版本正確的文件，解決 LLM 訓練資料過時的問題。

### 設計目標

本設計指南的目標是建立一個**通用型** Documentation MCP Server，不僅限於 Spring 文件，而是能夠支援任何技術文件：

- **多來源支援**：不限於特定框架或語言
- **版本感知**：支援多版本文件管理
- **語意搜尋**：提供智慧文件檢索
- **可擴展性**：模組化設計，易於擴展

### 為什麼需要？

| 問題 | 解決方案 |
|------|----------|
| LLM 訓練資料過時 | 即時注入最新文件 |
| 版本不匹配 | 版本感知的文件檢索 |
| 資訊碎片化 | 統一的文件存取介面 |
| 搜尋不精準 | 語意搜尋 + 全文搜尋 |

---

## 參考專案分析

### Context7

#### 概述

[Context7](https://github.com/upstash/context7) 是由 Upstash 開發的 MCP Server，專門將即時、版本特定的文件注入 LLM 的上下文視窗。

#### 架構特點

```
┌─────────────────────────────────────────────────────┐
│                    Context7                          │
├─────────────────────────────────────────────────────┤
│  Public MCP Server                                   │
│  └─ HTTP endpoint: https://mcp.context7.com/mcp     │
├─────────────────────────────────────────────────────┤
│  Private Backend (未開源)                            │
│  ├─ API Backend                                      │
│  ├─ Parsing Engine                                   │
│  └─ Crawling Engine                                  │
└─────────────────────────────────────────────────────┘
```

#### 核心工具

| 工具 | 功能 | 輸入 | 輸出 |
|------|------|------|------|
| `resolve-library-id` | 語意化函式庫匹配 | 函式庫名稱 | Context7 ID |
| `query-docs` | 文件檢索 | Library ID + Query | 文件片段 |

#### 設計模式

1. **語意匹配**：使用查詢排名和上下文化結果，而非精確字串匹配
2. **版本感知**：自動偵測 prompt 中的版本提及
3. **上下文注入**：文件直接流入 LLM prompt

#### 優點與限制

| 優點 | 限制 |
|------|------|
| 簡潔的 API 設計 | 核心解析引擎未開源 |
| 支援多種 IDE Client | 依賴外部服務 |
| 自動版本偵測 | 僅支援預定義的函式庫 |

---

### Spring Documentation MCP Server

#### 概述

[spring-documentation-mcp-server](https://github.com/andrlange/spring-documentation-mcp-server) 是一個功能完整的 Spring Boot MCP Server，專門提供 Spring 生態系文件。

#### 技術堆疊

| 組件 | 技術 |
|------|------|
| 語言 | Java 25 |
| 框架 | Spring Boot 3.5.9 |
| 資料庫 | PostgreSQL + pgvector |
| 建構工具 | Gradle |
| MCP 協議 | Streamable-HTTP (2025-11-25) |

#### 功能分類 (46 個工具)

```
Spring Documentation MCP Server
├── Documentation (12 tools)
│   ├── 同步 Spring 專案文件
│   ├── 版本追蹤
│   └── 全文搜尋 (tsvector)
│
├── Code Examples (搜尋程式碼片段)
│   ├── 語法高亮
│   └── 主題/類別分類
│
├── Migration Recipes (7 tools)
│   ├── OpenRewrite 啟發的轉換知識
│   └── 支援 55+ Spring 專案
│
├── Language Evolution (7 tools)
│   ├── Java 8+ 特性追蹤
│   └── Kotlin 1.6+ 特性追蹤
│
├── Flavors (8 tools)
│   ├── 公司特定指南
│   └── 架構模式/合規規則
│
├── Flavor Groups (3 tools)
│   └── 團隊存取控制
│
├── Boot Initializr (5 tools)
│   ├── start.spring.io 整合
│   └── 依賴搜尋/版本相容性
│
└── Javadoc APIs (4 tools)
    └── 索引化 Javadoc 全文搜尋
```

#### 關鍵設計決策

1. **PostgreSQL + pgvector**：結合全文搜尋與向量嵌入
2. **排程同步**：Cron 排程更新文件
3. **API Key 認證**：支援多種認證方式
4. **功能開關**：可選組件的 Feature Toggle

---

## 架構設計

### 整體架構

```
┌─────────────────────────────────────────────────────────────────┐
│                    Documentation MCP Server                      │
├─────────────────────────────────────────────────────────────────┤
│                        MCP Layer                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Tools     │  │  Resources  │  │   Prompts   │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
├─────────┼────────────────┼────────────────┼─────────────────────┤
│         └────────────────┼────────────────┘                      │
│                    Service Layer                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  Document   │  │   Search    │  │   Version   │              │
│  │  Service    │  │   Service   │  │   Service   │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
├─────────┴────────────────┴────────────────┴─────────────────────┤
│                    Repository Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  Document   │  │   Index     │  │   Config    │              │
│  │  Repository │  │  Repository │  │  Repository │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
├─────────┴────────────────┴────────────────┴─────────────────────┤
│                    Storage Layer                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  PostgreSQL (Full-text) + pgvector (Semantic Search)    │    │
│  └─────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                    Ingestion Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   GitHub    │  │   Web       │  │   Local     │              │
│  │   Fetcher   │  │   Crawler   │  │   Loader    │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### 模組化設計原則

#### 1. 文件來源抽象

```java
public interface DocumentSource {
    String getSourceId();
    SourceType getType();
    List<Document> fetchDocuments(FetchOptions options);
    boolean supportsVersioning();
}

// 實作範例
public class GitHubDocumentSource implements DocumentSource { ... }
public class WebCrawlerSource implements DocumentSource { ... }
public class LocalFileSource implements DocumentSource { ... }
```

#### 2. 搜尋策略抽象

```java
public interface SearchStrategy {
    SearchResult search(SearchQuery query);
    boolean supportsSemanticSearch();
}

// 實作範例
public class FullTextSearchStrategy implements SearchStrategy { ... }
public class VectorSearchStrategy implements SearchStrategy { ... }
public class HybridSearchStrategy implements SearchStrategy { ... }
```

#### 3. 版本管理

```java
public interface VersionManager {
    List<Version> getAvailableVersions(String library);
    Version resolveVersion(String library, String versionHint);
    Version getLatestStable(String library);
}
```

---

## 核心功能模組

### 1. 文件管理模組

#### 文件來源類型

| 類型 | 說明 | 範例 |
|------|------|------|
| **GitHub Repository** | 從 GitHub repo 取得文件 | README, docs/, wiki |
| **Official Documentation** | 官方文件網站爬蟲 | spring.io, react.dev |
| **Local Files** | 本地文件檔案 | 專案內的 markdown |
| **API Documentation** | Javadoc, OpenAPI | 自動生成的 API 文件 |

#### 文件處理流程

```
Raw Content → Parser → Chunker → Embedder → Indexer
                ↓
          Markdown/HTML
          to structured
               ↓
          Split into
          semantic chunks
               ↓
          Generate vector
          embeddings
               ↓
          Store in DB
          with metadata
```

### 2. 搜尋模組

#### 搜尋類型

| 搜尋類型 | 技術 | 適用場景 |
|----------|------|----------|
| **全文搜尋** | PostgreSQL tsvector | 精確關鍵字匹配 |
| **語意搜尋** | pgvector + embeddings | 概念相似度查詢 |
| **混合搜尋** | 全文 + 語意加權 | 最佳整體效果 |

#### 相關性排名因素

1. 文字匹配分數
2. 語意相似度
3. 文件新鮮度
4. 版本匹配度
5. 來源信任度

### 3. 版本管理模組

#### 版本狀態

| 狀態 | 說明 |
|------|------|
| `latest` | 最新穩定版本 |
| `default` | 預設使用版本 |
| `lts` | 長期支援版本 |
| `eol` | 生命週期結束 |
| `preview` | 預覽/測試版本 |

#### 版本解析邏輯

```java
public Version resolveVersion(String library, String hint) {
    // 1. 精確匹配
    if (exactMatch(hint)) return getVersion(library, hint);

    // 2. 主版本匹配 (如 "v15" → "v15.1.2")
    if (majorVersionMatch(hint)) return getLatestInMajor(library, hint);

    // 3. 語意解析 (如 "latest", "stable")
    if (semanticHint(hint)) return resolveSemanticVersion(library, hint);

    // 4. 預設最新穩定版
    return getLatestStable(library);
}
```

---

## MCP 工具設計

### 推薦工具集

基於 Context7 和 spring-documentation-mcp-server 的分析，建議以下工具設計：

### 核心工具

#### 1. resolve_library

解析函式庫識別符。

```java
@McpTool(
    name = "resolve_library",
    description = "Resolve a library name to its canonical identifier. " +
                  "Use this to find the correct library ID before querying docs.")
public LibraryInfo resolveLibrary(
    @McpToolParam(description = "Library name (e.g., 'react', 'spring-boot')", required = true)
    String name,
    @McpToolParam(description = "Version hint (optional, e.g., 'v18', 'latest')", required = false)
    String version) {

    return libraryService.resolve(name, version);
}
```

**回傳範例：**
```json
{
  "id": "/facebook/react/v18.2.0",
  "name": "React",
  "version": "18.2.0",
  "description": "A JavaScript library for building user interfaces",
  "documentationUrl": "https://react.dev"
}
```

#### 2. search_docs

搜尋文件。

```java
@McpTool(
    name = "search_docs",
    description = "Search documentation across libraries. " +
                  "Returns relevant documentation chunks with context.")
public SearchResult searchDocs(
    @McpToolParam(description = "Search query", required = true)
    String query,
    @McpToolParam(description = "Library ID (from resolve_library)", required = false)
    String libraryId,
    @McpToolParam(description = "Maximum results (default: 5, max: 20)", required = false)
    Integer limit,
    @McpToolParam(description = "Include code examples", required = false)
    Boolean includeExamples) {

    return searchService.search(query, libraryId, limit, includeExamples);
}
```

#### 3. get_doc_content

取得特定文件內容。

```java
@McpTool(
    name = "get_doc_content",
    description = "Get the full content of a specific documentation page.")
public DocumentContent getDocContent(
    @McpToolParam(description = "Document URI or path", required = true)
    String docUri,
    @McpToolParam(description = "Section to focus on (optional)", required = false)
    String section) {

    return documentService.getContent(docUri, section);
}
```

#### 4. list_libraries

列出可用函式庫。

```java
@McpTool(
    name = "list_libraries",
    description = "List available libraries and their versions.")
public LibraryList listLibraries(
    @McpToolParam(description = "Category filter (e.g., 'frontend', 'backend')", required = false)
    String category,
    @McpToolParam(description = "Include version details", required = false)
    Boolean includeVersions) {

    return libraryService.list(category, includeVersions);
}
```

### 進階工具

#### 5. get_api_reference

取得 API 參考。

```java
@McpTool(
    name = "get_api_reference",
    description = "Get API reference for a specific class, method, or function.")
public ApiReference getApiReference(
    @McpToolParam(description = "Library ID", required = true)
    String libraryId,
    @McpToolParam(description = "API identifier (e.g., class name, function name)", required = true)
    String apiName) {

    return apiService.getReference(libraryId, apiName);
}
```

#### 6. get_migration_guide

取得版本遷移指南。

```java
@McpTool(
    name = "get_migration_guide",
    description = "Get migration guide between two versions of a library.")
public MigrationGuide getMigrationGuide(
    @McpToolParam(description = "Library ID", required = true)
    String libraryId,
    @McpToolParam(description = "Source version", required = true)
    String fromVersion,
    @McpToolParam(description = "Target version", required = true)
    String toVersion) {

    return migrationService.getGuide(libraryId, fromVersion, toVersion);
}
```

#### 7. get_code_examples

取得程式碼範例。

```java
@McpTool(
    name = "get_code_examples",
    description = "Get code examples for a specific topic or API.")
public CodeExamples getCodeExamples(
    @McpToolParam(description = "Library ID", required = true)
    String libraryId,
    @McpToolParam(description = "Topic or API name", required = true)
    String topic,
    @McpToolParam(description = "Programming language filter", required = false)
    String language) {

    return exampleService.getExamples(libraryId, topic, language);
}
```

### 工具設計原則

| 原則 | 說明 |
|------|------|
| **語意化命名** | 使用動詞 + 名詞格式 (如 `search_docs`, `get_api_reference`) |
| **漸進式查詢** | 先 resolve → 再 search/get |
| **合理預設值** | 參數有預設值減少必填項 |
| **限制輸出大小** | 設定 max limit 防止過大回應 |
| **版本感知** | 所有查詢都支援版本指定 |

---

## 資料儲存方案

### 推薦方案：PostgreSQL + pgvector

#### 為什麼選擇這個組合？

| 需求 | PostgreSQL 解決方案 |
|------|---------------------|
| 全文搜尋 | tsvector + GIN 索引 |
| 語意搜尋 | pgvector 擴展 |
| 關聯查詢 | 標準 SQL JOIN |
| ACID 交易 | 原生支援 |
| 生態系統 | Spring Data JPA 整合 |

#### Schema 設計

```sql
-- 函式庫表
CREATE TABLE libraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    source_type VARCHAR(50) NOT NULL,
    source_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 版本表
CREATE TABLE library_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id UUID REFERENCES libraries(id),
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    release_date DATE,
    is_latest BOOLEAN DEFAULT FALSE,
    is_lts BOOLEAN DEFAULT FALSE,
    UNIQUE(library_id, version)
);

-- 文件表
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_version_id UUID REFERENCES library_versions(id),
    uri TEXT NOT NULL,
    title VARCHAR(500),
    content TEXT,
    content_type VARCHAR(100),
    search_vector tsvector,
    embedding vector(1536),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 全文搜尋索引
CREATE INDEX idx_documents_search ON documents USING GIN(search_vector);

-- 向量搜尋索引
CREATE INDEX idx_documents_embedding ON documents USING ivfflat(embedding vector_cosine_ops);

-- 自動更新 search_vector
CREATE TRIGGER update_search_vector
BEFORE INSERT OR UPDATE ON documents
FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(search_vector, 'pg_catalog.english', title, content);
```

#### 混合搜尋查詢

```sql
-- 混合搜尋：結合全文搜尋和語意搜尋
WITH text_search AS (
    SELECT
        id,
        ts_rank(search_vector, plainto_tsquery('english', $1)) AS text_score
    FROM documents
    WHERE search_vector @@ plainto_tsquery('english', $1)
),
vector_search AS (
    SELECT
        id,
        1 - (embedding <=> $2::vector) AS vector_score
    FROM documents
    ORDER BY embedding <=> $2::vector
    LIMIT 100
)
SELECT
    d.*,
    COALESCE(ts.text_score, 0) * 0.4 + COALESCE(vs.vector_score, 0) * 0.6 AS combined_score
FROM documents d
LEFT JOIN text_search ts ON d.id = ts.id
LEFT JOIN vector_search vs ON d.id = vs.id
WHERE ts.id IS NOT NULL OR vs.id IS NOT NULL
ORDER BY combined_score DESC
LIMIT $3;
```

---

## 最佳實踐

### ✅ 推薦做法 (Do's)

#### 1. 分層的工具設計

```
Level 1: Discovery (發現)
├── list_libraries
└── resolve_library

Level 2: Search (搜尋)
├── search_docs
└── get_api_reference

Level 3: Retrieve (獲取)
├── get_doc_content
├── get_code_examples
└── get_migration_guide
```

#### 2. 智慧的上下文大小管理

```java
@McpTool(name = "search_docs", description = "...")
public SearchResult searchDocs(
    @McpToolParam(description = "Max tokens in response (default: 5000)", required = false)
    Integer maxTokens) {

    int limit = maxTokens != null ? maxTokens : 5000;
    // 根據 token 限制調整回傳內容
    return searchService.searchWithTokenLimit(query, limit);
}
```

#### 3. 漸進式資訊揭露

```java
// 第一步：概述
SearchResult summary = searchDocs("react hooks", null, 3, false);

// 第二步：詳細內容
DocumentContent detail = getDocContent(summary.getTopResult().getUri(), "useState");

// 第三步：程式碼範例
CodeExamples examples = getCodeExamples("react", "useState", "typescript");
```

#### 4. 快取策略

| 資料類型 | 快取時間 | 理由 |
|----------|----------|------|
| 函式庫列表 | 1 小時 | 不常變動 |
| 版本資訊 | 15 分鐘 | 可能有新版本 |
| 文件內容 | 24 小時 | 相對穩定 |
| 搜尋結果 | 5 分鐘 | 可能有索引更新 |

### ❌ 應避免的做法 (Don'ts)

1. **不要**回傳過大的文件（設定 token 限制）
2. **不要**在單一工具中混合多個職責
3. **不要**忽略版本資訊
4. **不要**硬編碼文件來源 URL
5. **不要**跳過輸入驗證

### 💡 實務技巧 (Tips)

| 技巧 | 說明 |
|------|------|
| **Chunk Overlap** | 文件切割時保留重疊，避免上下文丟失 |
| **Metadata Enrichment** | 為文件添加豐富的元資料（標籤、類別、難度） |
| **Fallback Search** | 語意搜尋無結果時退回全文搜尋 |
| **Usage Analytics** | 追蹤工具使用頻率以優化設計 |

### ⚠️ 常見錯誤與陷阱

| 錯誤 | 後果 | 解決方案 |
|------|------|----------|
| 未處理版本不存在 | 錯誤的文件內容 | 實作版本 fallback 邏輯 |
| 無限制的搜尋結果 | 記憶體爆炸 | 強制設定 max limit |
| 同步載入大型文件 | 超時 | 使用非同步載入 + 分頁 |
| 未實作快取 | 重複查詢效能差 | 使用 Caffeine 等快取 |

---

## 實作建議

### 技術選型

| 組件 | 推薦技術 | 理由 |
|------|----------|------|
| 框架 | Spring Boot 4.0.1 + Spring AI 2.0.0-M1 | 官方 MCP 支援 |
| 語言 | Java 25 | Virtual Threads 支援、最新 LTS |
| 建構工具 | Gradle | 靈活的建構配置 |
| 資料庫 | PostgreSQL + pgvector | 全文 + 向量搜尋 |
| 快取 | Caffeine | 高效能本地快取 |
| 嵌入模型 | OpenAI / Ollama | 向量嵌入生成 |
| 文件解析 | Jsoup + flexmark | HTML/Markdown 解析 |
| 開發環境 | Docker Compose | 本地開發環境 |
| 測試 | Testcontainers | 整合測試 |

### 實作路線圖

#### Phase 1: 基礎功能
- [ ] 專案初始化 (Spring Boot + Spring AI MCP)
- [ ] 資料庫 Schema 設計與實作
- [ ] 基本 CRUD Repository
- [ ] 核心工具實作 (resolve_library, search_docs, get_doc_content)

#### Phase 2: 搜尋強化
- [ ] 全文搜尋實作
- [ ] 向量嵌入生成
- [ ] 混合搜尋整合
- [ ] 搜尋結果排名優化

#### Phase 3: 文件同步
- [ ] GitHub 文件抓取器
- [ ] 網頁爬蟲 (官方文件)
- [ ] 排程同步任務
- [ ] 增量更新機制

#### Phase 4: 進階功能
- [ ] API 文件支援 (Javadoc, OpenAPI)
- [ ] 版本遷移指南
- [ ] 程式碼範例庫
- [ ] 多語言支援

#### Phase 5: 生產就緒
- [ ] 認證與授權
- [ ] 監控與日誌
- [ ] 效能優化
- [ ] 文件與部署指南

---

## 參考來源

| 優先級 | 來源類型 | 名稱 | 說明 | 連結 |
|--------|----------|------|------|------|
| 🥇 | 官方 GitHub | Context7 | Upstash 的 Documentation MCP Server | [GitHub](https://github.com/upstash/context7) |
| 🥇 | 官方 GitHub | spring-documentation-mcp-server | Spring 文件 MCP Server 參考實作 | [GitHub](https://github.com/andrlange/spring-documentation-mcp-server) |
| 🥇 | 官方文件 | MCP Specification | MCP 協議規格 | [modelcontextprotocol.io](https://modelcontextprotocol.io/specification/2025-11-25) |
| 🥇 | 官方文件 | Spring AI MCP Reference | Spring AI MCP 參考文件 | [Spring.io](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) |
| 🥈 | 企業部落格 | Upstash Blog - Context7 | Context7 設計說明 | [Upstash](https://upstash.com/blog/context7-mcp) |
| 🥉 | 英文文章 | MCP Best Practices | MCP 最佳實踐指南 | [modelcontextprotocol.info](https://modelcontextprotocol.info/docs/best-practices/) |
| 4️⃣ | 社群 | Smithery - Context7 | Context7 社群頁面 | [Smithery](https://smithery.ai/server/@upstash/context7-mcp) |

---

## 決策記錄

| 日期 | 決策項目 | 選擇 | 原因 |
|------|----------|------|------|
| 2026-01-19 | 資料庫 | PostgreSQL + pgvector | 同時支援全文與向量搜尋 |
| 2026-01-19 | 傳輸協議 | Streamable-HTTP | MCP 2025-11-25 推薦，替代 SSE |
| 2026-01-19 | 工具設計 | 分層式 (Discovery → Search → Retrieve) | 符合 AI 代理漸進式查詢模式 |
| 2026-01-19 | 版本管理 | 多版本並存 | 支援不同專案需求 |
