# Spring Documentation MCP Server 深度分析

> 最後更新：2026-01-19
> 研究版本：1.8.1
> 來源：https://github.com/andrlange/spring-documentation-mcp-server
>
> https://deepwiki.com/andrlange/spring-documentation-mcp-server/1-overview

---

## 1. 專案概述

### 1.1 基本資訊

| 項目 | 說明 |
|------|------|
| **名稱** | Spring Documentation MCP Server |
| **版本** | 1.8.1 |
| **作者** | andrlange (使用 Claude Code 開發) |
| **授權** | MIT |
| **性質** | 非官方、教育用途 |
| **MCP 協議** | 2025-11-25 (Streamable-HTTP) |

### 1.2 專案定位

這是一個功能完整的 MCP Server 參考實作，展示如何使用 Spring Boot 建立文件服務給 AI 助手。專案明確聲明為「非官方」、「教育用途」，但其架構設計和功能完整度具有很高的參考價值。

### 1.3 技術堆疊

| 組件 | 技術 | 說明 |
|------|------|------|
| 語言 | Java 25 | 使用 Virtual Threads |
| 框架 | Spring Boot 3.5.9 | 最新穩定版 |
| MCP 實作 | Spring AI MCP | Streamable-HTTP |
| 資料庫 | PostgreSQL + pgvector | 全文 + 向量搜尋 |
| 建構工具 | Gradle | - |
| Web UI | Thymeleaf | 管理介面 |
| 容器化 | Docker Compose | 開發環境 |

---

## 2. 架構設計

### 2.1 整體架構

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Documentation MCP Server                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     MCP Layer (46 Tools)                         │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐        │    │
│  │  │   Docs    │ │ Migration │ │ Language  │ │  Flavors  │        │    │
│  │  │  (12)     │ │   (7)     │ │   (7)     │ │   (8)     │        │    │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘        │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐                      │    │
│  │  │  Groups   │ │ Initializr│ │  Javadoc  │                      │    │
│  │  │   (3)     │ │   (5)     │ │   (4)     │                      │    │
│  │  └───────────┘ └───────────┘ └───────────┘                      │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                   │                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      Web UI (Thymeleaf)                          │    │
│  │  Dashboard │ Projects │ Docs │ Flavors │ Settings │ Monitor     │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                   │                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                       Service Layer                              │    │
│  │  DocumentService │ SearchService │ SyncService │ EmbeddingService│    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                   │                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      Storage Layer                               │    │
│  │  ┌─────────────────────────────────────────────────────────┐    │    │
│  │  │                   PostgreSQL                             │    │    │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │    │    │
│  │  │  │  tsvector   │  │  pgvector   │  │   Relational    │  │    │    │
│  │  │  │ (Full-text) │  │ (Semantic)  │  │    (CRUD)       │  │    │    │
│  │  │  └─────────────┘  └─────────────┘  └─────────────────┘  │    │    │
│  │  └─────────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 MCP 端點

```
HTTP POST/GET → /mcp/spring → Streamable-HTTP Protocol
                    │
                    ├── tools/list
                    ├── tools/call
                    ├── resources/list
                    └── resources/read
```

### 2.3 Virtual Threads 架構

專案充分利用 Java 21+ Virtual Threads (JEP 444)：

| Executor | 用途 |
|----------|------|
| `virtualThreadExecutor` | 預設非同步操作 |
| `taskExecutor` | 通用任務 |
| `indexingExecutor` | 文件索引 |
| `bootstrapExecutor` | 啟動任務 |
| `embeddingTaskExecutor` | Embedding 生成 |

**優勢**：
- 傳統 Platform Thread: ~1MB stack
- Virtual Thread: ~1KB memory
- 支援數百萬並發操作

---

## 3. MCP 工具詳細分析

### 3.1 工具總覽 (46 個工具)

```
┌────────────────────────────────────────────────────────────┐
│                    MCP Tools (46 Total)                     │
├─────────────────┬──────────────────────────────────────────┤
│ Category        │ Tools                                     │
├─────────────────┼──────────────────────────────────────────┤
│ Documentation   │ 12 tools - 文件搜尋、版本管理、程式碼範例  │
│ Migration       │  7 tools - 版本遷移指南、breaking changes │
│ Language        │  7 tools - Java/Kotlin 特性追蹤          │
│ Flavors         │  8 tools - 公司指南、架構模式             │
│ Flavor Groups   │  3 tools - 團隊存取控制                   │
│ Boot Initializr │  5 tools - start.spring.io 整合          │
│ Javadoc APIs    │  4 tools - API 文件搜尋                   │
└─────────────────┴──────────────────────────────────────────┘
```

### 3.2 Documentation Tools (12 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `searchDocumentation` | 全文搜尋文件 | query, project, version, limit |
| `getDocumentationById` | 取得特定文件 | documentId |
| `listProjects` | 列出所有專案 | - |
| `getProjectVersions` | 取得專案版本列表 | projectName |
| `getLatestVersion` | 取得最新版本 | projectName |
| `searchCodeExamples` | 搜尋程式碼範例 | query, language, category |
| `getCodeExampleById` | 取得特定程式碼 | exampleId |
| `listCodeCategories` | 列出程式碼分類 | - |
| `getDocsByVersion` | 取得版本文件 | project, version |
| `getRelatedDocs` | 取得相關文件 | documentId |
| `getDocToc` | 取得文件目錄 | documentId |
| `semanticSearch` | 語意搜尋 | query, limit, threshold |

### 3.3 Migration Tools (7 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `getMigrationRecipes` | 取得遷移配方 | fromVersion, toVersion, project |
| `searchMigrationRecipes` | 搜尋遷移配方 | query, severity |
| `getBreakingChanges` | 取得 breaking changes | fromVersion, toVersion |
| `getUpgradePath` | 取得升級路徑 | currentVersion, targetVersion |
| `getMigrationByProject` | 取得專案遷移 | projectName |
| `getRecipeById` | 取得特定配方 | recipeId |
| `listMigrationProjects` | 列出支援的專案 | - |

**遷移配方嚴重性等級**：
- `CRITICAL`: 必須修改，否則無法編譯/運行
- `ERROR`: 嚴重問題，需要立即處理
- `WARNING`: 潛在問題，建議修改
- `INFO`: 建議改善，非必要

### 3.4 Language Evolution Tools (7 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `getJavaFeatures` | 取得 Java 特性 | version, status |
| `getKotlinFeatures` | 取得 Kotlin 特性 | version, status |
| `compareJavaVersions` | 比較 Java 版本 | fromVersion, toVersion |
| `compareKotlinVersions` | 比較 Kotlin 版本 | fromVersion, toVersion |
| `searchLanguageFeatures` | 搜尋語言特性 | query, language |
| `getFeatureById` | 取得特定特性 | featureId |
| `getDeprecations` | 取得棄用列表 | language, version |

**特性狀態**：
- `NEW`: 新增特性
- `DEPRECATED`: 已棄用
- `REMOVED`: 已移除
- `PREVIEW`: 預覽功能
- `INCUBATING`: 孵化中

### 3.5 Flavors Tools (8 個)

這是專案最獨特的功能之一，用於管理公司/團隊特定的指南。

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `searchFlavors` | 全文搜尋指南 | query, category, limit |
| `getFlavorByName` | 取得特定指南 | uniqueName |
| `getFlavorsByCategory` | 取得分類指南 | category, activeOnly |
| `getArchitecturePatterns` | 取得架構模式 | technology, pattern |
| `getComplianceRules` | 取得合規規則 | framework, domain |
| `getAgentConfiguration` | 取得 AI 代理配置 | useCase, language |
| `getProjectInitialization` | 取得專案初始化 | projectType, features |
| `listFlavorCategories` | 列出所有分類 | - |

**Flavor 分類**：

| 分類 | 用途 | 範例 |
|------|------|------|
| `ARCHITECTURE` | 架構模式 | Hexagonal, CQRS, DDD |
| `COMPLIANCE` | 合規要求 | GDPR, SOC2, HIPAA, PCI-DSS |
| `AGENTS` | AI 代理配置 | Code Review, Documentation |
| `INITIALIZATION` | 專案初始化 | Spring Boot, Microservice |
| `GENERAL` | 通用指南 | Coding Standards |

**Flavor YAML 格式**：

```yaml
---
unique-name: hexagonal-architecture
display-name: Hexagonal Architecture Pattern
category: ARCHITECTURE
tags:
  - architecture
  - ddd
  - ports-and-adapters
---
# Hexagonal Architecture

## Overview
Hexagonal architecture (ports and adapters)...

## Implementation Guidelines
...
```

### 3.6 Flavor Groups Tools (3 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `listFlavorGroups` | 列出所有群組 | - |
| `getFlavorGroupById` | 取得群組詳情 | groupId |
| `getGroupFlavors` | 取得群組指南 | groupId |

**存取控制模型**：
- **Public Group** (無成員)：所有 API Key 可見
- **Private Group** (有成員)：僅成員 API Key 可見

### 3.7 Boot Initializr Tools (5 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `searchDependencies` | 搜尋依賴 | query, bootVersion |
| `getDependencyById` | 取得依賴詳情 | dependencyId |
| `checkCompatibility` | 檢查相容性 | dependencies, bootVersion |
| `listCategories` | 列出依賴分類 | - |
| `generateBuildFile` | 生成建構檔案 | dependencies, buildTool |

**特色功能**：
- 即時 start.spring.io 整合
- 版本相容性自動檢查
- 不相容警告提示
- Caffeine 快取 (可配置 TTL)
- Maven/Gradle snippet 生成

### 3.8 Javadoc API Tools (4 個)

| 工具名稱 | 功能 | 關鍵參數 |
|----------|------|----------|
| `searchJavadoc` | 搜尋 API 文件 | query, project, type |
| `getClassDoc` | 取得類別文件 | className, project |
| `getPackageDoc` | 取得套件文件 | packageName, project |
| `listJavadocLibraries` | 列出可用函式庫 | - |

**爬蟲行為**：
- 預設同步 GA 和 CURRENT 版本
- 可過濾 SNAPSHOT, RC, Milestone
- 請求間隔 230ms 防止過載
- 連續 5 次失敗自動停用
- 每週日凌晨 4 點排程同步

---

## 4. 資料儲存設計

### 4.1 搜尋架構

專案實作**混合搜尋 (Hybrid Search)**，結合全文搜尋和語意搜尋：

```
┌─────────────────────────────────────────────────────────────┐
│                      Hybrid Search                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Query: "How to configure Spring Security?"                │
│                         │                                    │
│           ┌─────────────┼─────────────┐                     │
│           ▼                           ▼                      │
│   ┌───────────────┐           ┌───────────────┐             │
│   │   Full-Text   │           │   Semantic    │             │
│   │   (tsvector)  │           │   (pgvector)  │             │
│   ├───────────────┤           ├───────────────┤             │
│   │ plainto_tsquery│          │ Cosine        │             │
│   │ ts_rank_cd()  │           │ Similarity    │             │
│   └───────┬───────┘           └───────┬───────┘             │
│           │                           │                      │
│           │    keyword_score          │   semantic_score     │
│           │                           │                      │
│           └─────────────┬─────────────┘                     │
│                         ▼                                    │
│   ┌─────────────────────────────────────────────────────┐   │
│   │  final_score = (1-α) × semantic + α × keyword       │   │
│   │  Default α = 0.3 (70% semantic, 30% keyword)        │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Embedding 配置

| Provider | Model | Dimensions | Cost |
|----------|-------|------------|------|
| **Ollama** | nomic-embed-text | 768 | Free |
| **OpenAI** | text-embedding-3-small | 1536 | Paid |
| **OpenAI** | text-embedding-ada-002 | 1536 | Paid |

**配置範例**：

```yaml
mcp:
  features:
    embeddings:
      enabled: true
      provider: ollama  # or openai
      hybrid:
        alpha: 0.3      # keyword weight
        min-similarity: 0.5
```

### 4.3 資料表設計推測

基於功能分析，推測的核心資料表：

```sql
-- 專案/函式庫
CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    github_url TEXT,
    docs_url TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 版本
CREATE TABLE project_versions (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    version VARCHAR(50) NOT NULL,
    status VARCHAR(20),  -- GA, SNAPSHOT, EOL
    is_current BOOLEAN,
    release_date DATE,
    synced_at TIMESTAMP,
    UNIQUE(project_id, version)
);

-- 文件
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    version_id UUID REFERENCES project_versions(id),
    path TEXT NOT NULL,
    title VARCHAR(500),
    content TEXT,
    content_html TEXT,
    search_vector tsvector,
    metadata JSONB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 文件區塊 (用於 embedding)
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id),
    chunk_index INTEGER,
    content TEXT,
    embedding vector(768),  -- or 1536 for OpenAI
    metadata JSONB
);

-- 程式碼範例
CREATE TABLE code_examples (
    id UUID PRIMARY KEY,
    version_id UUID REFERENCES project_versions(id),
    title VARCHAR(255),
    description TEXT,
    code TEXT,
    language VARCHAR(50),
    category VARCHAR(100),
    tags TEXT[],
    search_vector tsvector
);

-- 遷移配方
CREATE TABLE migration_recipes (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    from_version VARCHAR(50),
    to_version VARCHAR(50),
    title VARCHAR(255),
    description TEXT,
    severity VARCHAR(20),  -- CRITICAL, ERROR, WARNING, INFO
    before_code TEXT,
    after_code TEXT,
    search_vector tsvector
);

-- Flavors
CREATE TABLE flavors (
    id UUID PRIMARY KEY,
    unique_name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(255),
    category_id UUID REFERENCES flavor_categories(id),
    content TEXT,  -- Markdown
    tags TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    indexed_content tsvector,
    embedding vector(768),
    created_by VARCHAR(100),
    created_at TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP
);

-- Flavor 分類
CREATE TABLE flavor_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Flavor 群組
CREATE TABLE flavor_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE
);

-- 群組成員
CREATE TABLE flavor_group_members (
    group_id UUID REFERENCES flavor_groups(id),
    api_key_id UUID REFERENCES api_keys(id),
    PRIMARY KEY (group_id, api_key_id)
);

-- API Keys
CREATE TABLE api_keys (
    id UUID PRIMARY KEY,
    key_hash VARCHAR(255) NOT NULL,  -- BCrypt
    name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    last_used_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_documents_search ON documents USING GIN(search_vector);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING ivfflat(embedding vector_cosine_ops);
CREATE INDEX idx_flavors_indexed ON flavors USING GIN(indexed_content);
CREATE INDEX idx_flavors_category ON flavors(category_id);
CREATE INDEX idx_flavors_active ON flavors(is_active) WHERE is_active = TRUE;
```

---

## 5. 安全機制

### 5.1 API Key 認證

**Key 格式**：`smcp_<256-bit-random-string>`

**儲存**：BCrypt 雜湊，永不明文儲存

**傳遞方式**（優先順序）：
1. `X-API-Key` Header（推薦）
2. `Authorization: Bearer <key>` Header
3. Query Parameter（僅測試用）

### 5.2 Web UI 認證

- Spring Security
- 預設帳號：admin/admin
- 角色：Admin, User, ReadOnly

### 5.3 存取控制

| 操作 | Web UI | MCP |
|------|--------|-----|
| 檢視 | 需登入 | 需 API Key |
| 建立/編輯/刪除 | ADMIN 角色 | - |

---

## 6. 配置參考

### 6.1 核心環境變數

```bash
# 資料庫
DB_HOST=localhost
DB_PORT=5432
DB_NAME=springdocs
DB_USER=springdocs
DB_PASSWORD=springdocs

# 管理員
ADMIN_USER=admin
ADMIN_PASSWORD=admin

# 伺服器
SERVER_PORT=8080

# 啟動載入範例資料
BOOTSTRAP_DOCS=true

# GitHub Token (提高 Rate Limit)
GITHUB_TOKEN=ghp_xxxxxxxxxxxx
```

### 6.2 完整 application.yml

```yaml
spring:
  application:
    name: spring-documentation-mcp-server

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:springdocs}
    username: ${DB_USER:springdocs}
    password: ${DB_PASSWORD:springdocs}

  ai:
    mcp:
      server:
        protocol: STREAMABLE
        version: "1.8.1"

mcp:
  features:
    # OpenRewrite 遷移配方
    openrewrite:
      enabled: true

    # 語言演進追蹤
    language-evolution:
      enabled: true

    # 公司指南
    flavors:
      enabled: true

    # Spring Initializr 整合
    initializr:
      enabled: true
      cache-ttl: 3600  # seconds
      base-url: https://start.spring.io

    # Javadoc 爬蟲
    javadoc:
      enabled: true
      sync-schedule: "0 0 4 * * SUN"  # 每週日 4 AM
      request-delay: 230  # ms

    # Embedding 配置
    embeddings:
      enabled: true
      provider: ollama  # or openai
      ollama:
        base-url: http://localhost:11434
        model: nomic-embed-text
      openai:
        api-key: ${OPENAI_API_KEY:}
        model: text-embedding-3-small
      hybrid:
        alpha: 0.3
        min-similarity: 0.5

  # 文件同步
  sync:
    enabled: true
    cron: "0 0 2 * * *"  # 每天凌晨 2 點
    github:
      token: ${GITHUB_TOKEN:}
      rate-limit-buffer: 100

  # 監控
  monitoring:
    enabled: true
    retention-hours: 24
```

---

## 7. 對 DocMCP Server 的啟示

### 7.1 值得採用的設計

| 設計 | 說明 | 適用性 |
|------|------|--------|
| **混合搜尋** | tsvector + pgvector 結合 | ✅ 高 |
| **Flavors 系統** | 可自定義的指南管理 | ✅ 中 |
| **API Key 認證** | BCrypt + 多種傳遞方式 | ✅ 高 |
| **Virtual Threads** | 高併發 I/O 操作 | ✅ 高 |
| **Tool Masquerading** | 動態工具可見性 | ⚠️ 可選 |
| **監控 Dashboard** | 工具使用追蹤 | ⚠️ 可選 |

### 7.2 可簡化的部分

| 功能 | 簡化建議 |
|------|----------|
| 46 個工具 | 初期專注 10-15 個核心工具 |
| Migration Recipes | 第二階段再實作 |
| Language Evolution | 第二階段再實作 |
| Javadoc 爬蟲 | 第二階段再實作 |
| Boot Initializr | 視需求決定 |

### 7.3 建議的 MVP 工具集

基於分析，建議 DocMCP Server MVP 包含：

```
Phase 1 MVP (12 tools):
├── Discovery (2)
│   ├── list_libraries
│   └── resolve_library
├── Search (3)
│   ├── search_docs
│   ├── semantic_search
│   └── get_doc_content
├── Version (2)
│   ├── list_versions
│   └── get_latest_version
├── Code Examples (2)
│   ├── search_code_examples
│   └── get_code_example
└── Management (3)
    ├── sync_library
    ├── get_sync_status
    └── list_categories
```

---

## 8. 參考來源

| 類型 | 名稱 | 連結 |
|------|------|------|
| 🥇 官方 GitHub | spring-documentation-mcp-server | [GitHub](https://github.com/andrlange/spring-documentation-mcp-server) |
| 🥉 英文文章 | DeepWiki - Flavors Tools | [DeepWiki](https://deepwiki.com/andrlange/spring-documentation-mcp-server/3.4-flavors-tools-(8-tools)) |
| 4️⃣ 社群 | LobeHub - Spring Documentation MCP | [LobeHub](https://lobehub.com/mcp/kdmeenaa-spring-documentation-mcp-server) |

---

## 變更記錄

| 日期 | 變更 |
|------|------|
| 2026-01-19 | 初始深度分析 |
