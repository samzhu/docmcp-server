# CLAUDE.md

一率使用繁體中文跟用戶說明

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
│   └── parser/       # Document parsers (Markdown, AsciiDoc, HTML)
├── mcp/
│   ├── tool/         # MCP tools organized by layer
│   │   ├── discovery/  # list_libraries, resolve_library
│   │   ├── search/     # search_docs, semantic_search
│   │   └── retrieve/   # get_doc_content, get_code_examples
│   └── dto/          # MCP request/response DTOs
├── web/
│   ├── api/          # REST API controllers (LibraryApiController, SearchApiController, SyncApiController)
│   └── dto/          # Web DTOs
└── security/         # API key authentication (ApiKeyService, ApiKeyAuthenticationFilter)
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
- Registered tools: `list_libraries`, `resolve_library`, `search_docs`, `semantic_search`, `get_doc_content`, `get_code_examples`

## Database

Uses PostgreSQL with pgvector extension. Schema auto-initialized on startup (`spring.sql.init.mode=always`).

Key tables: `libraries`, `library_versions`, `documents`, `document_chunks` (with vector embeddings), `code_examples`, `sync_history`, `api_keys`

## Testing

Tests use Testcontainers (requires Docker). Test configuration in `src/test/resources/application-test.yaml`.

```bash
# Integration test for MCP
./gradlew test --tests "io.github.samzhu.docmcp.McpIntegrationTest"
```
