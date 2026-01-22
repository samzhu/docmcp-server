# CLAUDE.md

程式碼都要寫上好理解的繁體中文註解

## PRD 文件維護

**重要**：當實作新功能或修改現有功能時，必須同步更新 PRD 文件以保持一致性。
- **PRD 位置**：`docs/PRD.md`
- **更新變更記錄**：在 PRD 末尾的「變更記錄」表格新增一行

## Project Overview

DocMCP Server is a self-hosted MCP (Model Context Protocol) server that provides documentation to AI coding assistants. Built with Spring Boot 4.0.1, Spring AI 2.0.0-M1, and PostgreSQL + pgvector for hybrid search (full-text + semantic).

**Tech Stack**: Java 25, Spring Boot 4.0.1, Spring AI 2.0.0-M1 (WebMVC Stateless MCP), PostgreSQL + pgvector, Thymeleaf, Gradle

## Development Workflow (TDD)

This project follows Test-Driven Development. When implementing new features or fixing bugs:

1. **Red** - Write a failing test first that defines the expected behavior
2. **Green** - Write the minimal code to make the test pass
3. **Refactor** - Improve the code while keeping tests green

```bash
# TDD cycle: run specific test continuously while developing
./gradlew test --tests "YourNewTest" --continuous
```

Test placement conventions:
- Unit tests: `src/test/java/.../service/` or `src/test/java/.../domain/model/`
- Integration tests: `src/test/java/.../repository/` (uses Testcontainers)
- MCP tool tests: `src/test/java/.../mcp/tool/`

## Common Commands

```bash
# Run application (auto-starts PostgreSQL via Docker Compose)
./gradlew bootRun

# Run all tests (uses Testcontainers - Docker required)
./gradlew test

# Run a single test class
./gradlew test --tests "io.github.samzhu.docmcp.service.LibraryServiceTest"

# Run a single test method
./gradlew test --tests "io.github.samzhu.docmcp.service.LibraryServiceTest.shouldCreateLibrary"

# Build
./gradlew build

# Clean build
./gradlew clean build
```

## Architecture

```
src/main/java/io/github/samzhu/docmcp/
├── config/           # Spring configuration (SecurityConfig, McpToolsConfig, ExecutorConfig)
├── domain/
│   ├── model/        # Entities: Library, LibraryVersion, Document, DocumentChunk, CodeExample, ApiKey
│   ├── enums/        # SourceType, VersionStatus, SyncStatus, ApiKeyStatus
│   └── exception/    # Domain exceptions
├── repository/       # Spring Data JDBC repositories
├── service/          # Business logic (LibraryService, SearchService, EmbeddingService, SyncService)
├── infrastructure/
│   ├── github/       # GitHub API client for fetching docs
│   ├── local/        # Local file client
│   └── parser/       # Document parsers (Markdown, AsciiDoc, HTML)
├── mcp/
│   ├── tool/         # MCP tools organized by layer
│   │   ├── discovery/  # list_libraries, resolve_library
│   │   ├── search/     # search_docs, semantic_search, get_api_reference
│   │   ├── retrieve/   # get_doc_content, get_code_examples, get_doc_toc, get_related_docs, get_migration_guide
│   │   └── management/ # list_versions, get_sync_status
│   ├── resource/     # MCP resources (DocResourceProvider, LibraryResourceProvider)
│   ├── prompt/       # MCP prompts (DocMcpPrompts)
│   └── dto/          # MCP request/response DTOs
├── web/
│   ├── api/          # REST API controllers
│   └── dto/          # Web DTOs
├── security/         # API key authentication (ApiKeyService, ApiKeyAuthenticationFilter)
└── scheduler/        # Scheduled tasks (SyncScheduler)
```

## Key Design Patterns

- **MCP Tools Layer Pattern**: Discovery → Search → Retrieve (progressive query refinement)
- **Non-starter Spring AI**: Manual Bean configuration for EmbeddingModel and VectorStore (see config classes)
- **Virtual Threads**: Enabled via `spring.threads.virtual.enabled=true` for high concurrency
- **Hybrid Search**: Combines PostgreSQL tsvector (full-text) + pgvector (semantic), formula: `final_score = (1-α) × semantic + α × keyword`

## Configuration

- `src/main/resources/application.yaml` - Base config (bundled in image)
- `src/main/resources/application-local.yaml` - Local overrides
- `config/application-dev.yaml` - Dev profile with DEBUG logging
- `config/application-secrets.properties` - Local secrets (gitignored), requires `docmcp-google-api-key`

Environment variables for secrets:
- `docmcp-google-api-key` - Google GenAI API key for embeddings
- `docmcp-db-url`, `docmcp-db-username`, `docmcp-db-password` - Database connection

## MCP Endpoint

- `POST /mcp` - Stateless MCP JSON-RPC endpoint

**Tools (12 個)**：
- Discovery: `list_libraries`, `resolve_library`
- Search: `search_docs`, `semantic_search`, `get_api_reference`
- Retrieve: `get_doc_content`, `get_code_examples`, `get_doc_toc`, `get_related_docs`, `get_migration_guide`
- Management: `list_versions`, `get_sync_status`

**Resources (2 個 Provider)**：
- `docs://{libraryName}/{version}/{path}` - 文件內容
- `library://{libraryName}` - 函式庫元資料

**Prompts (5 個)**：
- `docmcp_explain_library` - 解釋函式庫核心概念
- `docmcp_search_usage` - 搜尋功能使用方式
- `docmcp_compare_versions` - 比較版本差異
- `docmcp_generate_example` - 生成程式碼範例
- `docmcp_troubleshoot` - 故障排除

## Database

Uses PostgreSQL with pgvector extension. Schema auto-initialized on startup (`spring.sql.init.mode=always`).

Key tables: `libraries`, `library_versions`, `documents`, `document_chunks` (with vector embeddings), `code_examples`, `sync_history`, `api_keys`

## Testing

Tests use Testcontainers (requires Docker). Test configuration in `src/test/resources/application-test.yaml`.

```bash
# Integration test for MCP
./gradlew test --tests "io.github.samzhu.docmcp.McpIntegrationTest"
```
