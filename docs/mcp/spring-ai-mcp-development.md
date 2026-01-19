# Spring AI 2.0.0-M1 MCP Server 開發指南

> 最後更新：2026-01-19
> 難度：中級
> 前置知識：Spring Boot、Java 21+、MCP 基本概念

## 目錄

1. [簡介](#簡介)
2. [環境配置](#環境配置)
3. [Server 類型與傳輸協議](#server-類型與傳輸協議)
4. [核心註解](#核心註解)
5. [快速開始](#快速開始)
6. [進階功能](#進階功能)
7. [最佳實踐](#最佳實踐)
8. [參考來源](#參考來源)

---

## 簡介

### 什麼是 Spring AI MCP Server Boot Starter？

Spring AI MCP Server Boot Starter 提供了在 Spring Boot 應用程式中設定 Model Context Protocol (MCP) Server 的自動配置支援。它實現了 MCP 協議，讓開發者可以使用宣告式 Java 註解來建立 MCP Server，而非手動配置。

### Spring AI 2.0.0-M1 亮點

Spring AI 2.0.0-M1 是 2.x 系列的第一個里程碑版本：

- 基於 **Spring Boot 4.0** 和 **Spring Framework 7.0**
- 需要 **Java 21+**
- 包含 24 個新功能、25 個 Bug 修復
- 增強的 MCP Client 自動配置
- 改進的 Bean 類型相容性

### 適合誰？

- 使用 Spring Boot 開發後端服務的開發者
- 希望為 AI 應用程式提供工具和資料的團隊
- 需要建立企業級 MCP Server 的架構師

---

## 環境配置

### 系統需求

| 需求 | 版本 |
|------|------|
| Java | 21+ (建議使用 LTS 25) |
| Spring Boot | 4.0+ |
| Spring AI | 2.0.0-M1+ |

### Gradle 依賴

根據傳輸協議選擇適當的 Starter：

#### STDIO Transport
```groovy
implementation 'org.springframework.ai:spring-ai-starter-mcp-server'
```

#### WebMVC Transport (HTTP/SSE) - 推薦
```groovy
implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
```

#### WebFlux Transport (Reactive)
```groovy
implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webflux'
```

### 完整 build.gradle 範例

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springAiVersion', "2.0.0-M1")
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'

    // 如需向量搜尋
    implementation 'org.springframework.ai:spring-ai-starter-vector-store-pgvector'

    developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Maven 依賴 (替代方案)

如果使用 Maven：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

---

## Server 類型與傳輸協議

### 傳輸協議選擇

| 傳輸協議 | Starter | 配置 | 說明 |
|----------|---------|------|------|
| **STDIO** | `spring-ai-starter-mcp-server` | `spring.ai.mcp.server.stdio=true` | 程序內通訊 |
| **SSE** | `*-webmvc` / `*-webflux` | `protocol=SSE` 或空 | 即時事件串流 |
| **Streamable-HTTP** | `*-webmvc` / `*-webflux` | `protocol=STREAMABLE` | HTTP POST/GET + 可選 SSE |
| **Stateless** | `*-webmvc` / `*-webflux` | `protocol=STATELESS` | 無狀態，適合微服務 |

### 同步 vs 非同步

| 類型 | 配置 | 實作類 | 適用場景 |
|------|------|--------|----------|
| **Sync** | `type=SYNC` | `McpSyncServer` | 直接的請求-回應模式 |
| **Async** | `type=ASYNC` | `McpAsyncServer` | 非阻塞操作，使用 Reactor |

### 配置範例

```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC                    # SYNC 或 ASYNC
        protocol: STREAMABLE          # SSE, STREAMABLE, STATELESS
        stdio: false                  # 是否啟用 STDIO
        annotation-scanner:
          enabled: true               # 啟用註解掃描
```

---

## 核心註解

### @McpTool

將方法標記為 MCP 工具，自動生成 JSON Schema。

#### 基本用法

```java
@Component
public class CalculatorTools {

    @McpTool(name = "add", description = "Add two numbers together")
    public int add(
            @McpToolParam(description = "First number", required = true) int a,
            @McpToolParam(description = "Second number", required = true) int b) {
        return a + b;
    }
}
```

#### 進階用法：帶註解元資料

```java
@McpTool(
    name = "calculate-area",
    description = "Calculate the area of a rectangle",
    annotations = @McpTool.McpAnnotations(
        title = "Rectangle Area Calculator",
        readOnlyHint = true,      // 唯讀操作
        destructiveHint = false,   // 非破壞性
        idempotentHint = true      // 冪等操作
    ))
public AreaResult calculateRectangleArea(
        @McpToolParam(description = "Width", required = true) double width,
        @McpToolParam(description = "Height", required = true) double height) {
    return new AreaResult(width * height, "square units");
}
```

#### 進階用法：帶請求上下文

```java
@McpTool(name = "process-data", description = "Process data with progress")
public String processData(
        McpSyncRequestContext context,
        @McpToolParam(description = "Data to process", required = true) String data) {

    // 記錄日誌
    context.info("Processing data: " + data);

    // 回報進度
    context.progress(p -> p.progress(0.5).total(1.0).message("Processing..."));

    // Ping 確認連接
    context.ping();

    return "Processed: " + data.toUpperCase();
}
```

#### 動態 Schema 支援

```java
@McpTool(name = "flexible-tool", description = "Process dynamic schema")
public CallToolResult processDynamic(CallToolRequest request) {
    Map<String, Object> args = request.arguments();
    String result = "Processed " + args.size() + " arguments dynamically";

    return CallToolResult.builder()
        .addTextContent(result)
        .build();
}
```

### @McpResource

提供透過 URI 模板存取資源的能力。

#### 基本用法

```java
@Component
public class ResourceProvider {

    @McpResource(
        uri = "config://{key}",
        name = "Configuration",
        description = "Provides configuration data")
    public String getConfig(String key) {
        return configData.get(key);
    }
}
```

#### 回傳 ReadResourceResult

```java
@McpResource(
    uri = "user-profile://{username}",
    name = "User Profile",
    description = "Provides user profile information")
public ReadResourceResult getUserProfile(String username) {
    String profileData = loadUserProfile(username);

    return new ReadResourceResult(List.of(
        new TextResourceContents(
            "user-profile://" + username,
            "application/json",
            profileData
        )
    ));
}
```

### @McpPrompt

生成 AI 互動的提示訊息。

#### 基本用法

```java
@Component
public class PromptProvider {

    @McpPrompt(
        name = "greeting",
        description = "Generate a greeting message")
    public GetPromptResult greeting(
            @McpArg(name = "name", description = "User's name", required = true)
            String name) {

        String message = "Hello, " + name + "! How can I help you today?";

        return new GetPromptResult(
            "Greeting",
            List.of(new PromptMessage(Role.ASSISTANT, new TextContent(message)))
        );
    }
}
```

#### 帶可選參數

```java
@McpPrompt(
    name = "personalized-message",
    description = "Generate a personalized message")
public GetPromptResult personalizedMessage(
        @McpArg(name = "name", required = true) String name,
        @McpArg(name = "age", required = false) Integer age,
        @McpArg(name = "interests", required = false) String interests) {

    StringBuilder message = new StringBuilder();
    message.append("Hello, ").append(name).append("!\n\n");

    if (age != null) {
        message.append("At ").append(age).append(" years old, ");
    }

    if (interests != null && !interests.isEmpty()) {
        message.append("Your interest in ").append(interests);
    }

    return new GetPromptResult(
        "Personalized Message",
        List.of(new PromptMessage(Role.ASSISTANT, new TextContent(message.toString())))
    );
}
```

### @McpComplete

提供提示的自動完成功能。

```java
@Component
public class CompletionProvider {

    @McpComplete(prompt = "city-search")
    public List<String> completeCityName(String prefix) {
        return cities.stream()
            .filter(city -> city.toLowerCase().startsWith(prefix.toLowerCase()))
            .limit(10)
            .toList();
    }

    @McpComplete(prompt = "travel-planner")
    public List<String> completeTravelDestination(CompleteRequest.CompleteArgument argument) {
        String prefix = argument.value().toLowerCase();
        String argumentName = argument.name();

        if ("city".equals(argumentName)) {
            return completeCities(prefix);
        } else if ("country".equals(argumentName)) {
            return completeCountries(prefix);
        }

        return List.of();
    }
}
```

---

## 快速開始

### Step 1：建立 Spring Boot 專案

```java
@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
```

### Step 2：配置 application.yml

```yaml
spring:
  ai:
    mcp:
      server:
        type: SYNC
        protocol: STREAMABLE
        annotation-scanner:
          enabled: true

server:
  port: 8080
```

### Step 3：建立工具類

```java
@Component
public class DocumentationTools {

    @McpTool(
        name = "search_docs",
        description = "Search documentation by keyword")
    public SearchResult searchDocs(
            @McpToolParam(description = "Search keyword", required = true)
            String keyword,
            @McpToolParam(description = "Maximum results", required = false)
            Integer limit) {

        int maxResults = limit != null ? limit : 10;
        // 執行搜尋邏輯
        return documentService.search(keyword, maxResults);
    }

    @McpTool(
        name = "get_doc",
        description = "Get documentation content by ID")
    public DocumentContent getDoc(
            @McpToolParam(description = "Document ID", required = true)
            String docId) {
        return documentService.findById(docId);
    }
}
```

### Step 4：建立資源提供者

```java
@Component
public class DocumentationResources {

    @McpResource(
        uri = "docs://{category}/{id}",
        name = "Documentation",
        description = "Access documentation by category and ID")
    public ReadResourceResult getDocumentation(String category, String id) {
        String content = documentService.getContent(category, id);

        return new ReadResourceResult(List.of(
            new TextResourceContents(
                "docs://" + category + "/" + id,
                "text/markdown",
                content
            )
        ));
    }
}
```

### Step 5：執行與測試

```bash
# 啟動應用程式
./mvnw spring-boot:run

# 測試 MCP 端點 (Streamable-HTTP)
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

---

## 進階功能

### 進度追蹤

```java
@McpTool(name = "long-task", description = "Long-running task with progress")
public String performLongTask(
        McpSyncRequestContext context,
        @McpToolParam(description = "Task name", required = true) String taskName) {

    String progressToken = context.request().progressToken();

    if (progressToken != null) {
        // 開始
        context.progress(p -> p.progress(0.0).total(1.0).message("Starting task"));

        // 處理中
        for (int i = 1; i <= 10; i++) {
            // 執行工作...
            context.progress(p -> p
                .progress(i * 0.1)
                .total(1.0)
                .message("Processing step " + i));
        }

        // 完成
        context.progress(p -> p.progress(1.0).total(1.0).message("Task completed"));
    }

    return "Task " + taskName + " completed";
}
```

### 非同步支援 (WebFlux)

```java
@Component
public class AsyncTools {

    @McpTool(name = "async-fetch", description = "Fetch data asynchronously")
    public Mono<String> asyncFetch(
            @McpToolParam(description = "URL", required = true) String url) {

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class);
    }

    @McpResource(uri = "async-data://{id}", name = "Async Data")
    public Mono<ReadResourceResult> asyncResource(String id) {
        return Mono.fromCallable(() -> {
            String data = loadData(id);
            return new ReadResourceResult(List.of(
                new TextResourceContents("async-data://" + id, "text/plain", data)
            ));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

### 上下文類型

| 上下文類型 | 說明 | 適用場景 |
|-----------|------|----------|
| `McpSyncRequestContext` | 同步請求完整上下文 | Stateful Sync Server |
| `McpAsyncRequestContext` | 非同步請求完整上下文 | Stateful Async Server |
| `McpTransportContext` | 輕量傳輸層上下文 | Stateless Server |

### 方法過濾規則

| Server 類型 | 接受的方法 | 過濾的方法 |
|-------------|-----------|-----------|
| **Sync Stateful** | 非響應式回傳 + 雙向上下文 | 響應式回傳 (Mono/Flux) |
| **Async Stateful** | 響應式回傳 + 雙向上下文 | 非響應式回傳 |
| **Sync Stateless** | 非響應式回傳 + 無雙向上下文 | 響應式或雙向上下文參數 |
| **Async Stateless** | 響應式回傳 + 無雙向上下文 | 非響應式或雙向上下文參數 |

---

## 最佳實踐

### ✅ 推薦做法 (Do's)

#### 1. 使用語意化的工具名稱

```java
// ✅ 好的命名
@McpTool(name = "search_documentation", description = "...")
@McpTool(name = "get_api_reference", description = "...")

// ❌ 避免的命名
@McpTool(name = "query", description = "...")
@McpTool(name = "fetch", description = "...")
```

#### 2. 提供詳細的描述

```java
@McpTool(
    name = "search_docs",
    description = "Search documentation across all categories. " +
                  "Supports full-text search with relevance ranking. " +
                  "Returns title, snippet, and URL for each result.")
```

#### 3. 適當使用工具註解

```java
@McpTool(
    name = "delete_document",
    description = "Delete a document permanently",
    annotations = @McpTool.McpAnnotations(
        destructiveHint = true,    // 標記為破壞性操作
        idempotentHint = false     // 非冪等
    ))
```

#### 4. 實作適當的錯誤處理

```java
@McpTool(name = "get_doc", description = "Get document by ID")
public CallToolResult getDocument(
        @McpToolParam(description = "Document ID", required = true) String id) {
    try {
        Document doc = documentService.findById(id);
        if (doc == null) {
            return CallToolResult.builder()
                .addTextContent("Document not found: " + id)
                .isError(true)
                .build();
        }
        return CallToolResult.builder()
            .addTextContent(doc.getContent())
            .build();
    } catch (Exception e) {
        return CallToolResult.builder()
            .addTextContent("Error retrieving document: " + e.getMessage())
            .isError(true)
            .build();
    }
}
```

### ❌ 應避免的做法 (Don'ts)

1. **不要**在 Stateless Server 中使用雙向上下文 (`McpSyncRequestContext`)
2. **不要**在描述中使用技術術語，應使用使用者友善的語言
3. **不要**建立過於通用的工具（如 "execute_query"）
4. **不要**忽略參數驗證
5. **不要**在同步方法中進行長時間阻塞操作

### 💡 實務技巧 (Tips)

| 技巧 | 說明 |
|------|------|
| **使用 Virtual Threads** | Java 21+ 支援，適合 I/O 密集型工具 |
| **實作快取** | 對常用查詢使用 Caffeine 等快取 |
| **結構化日誌** | 使用 MDC 追蹤請求 |
| **健康檢查** | 實作 `/actuator/health` 端點 |

### ⚠️ 常見錯誤與陷阱

| 錯誤 | 後果 | 解決方案 |
|------|------|----------|
| 混用同步/非同步方法 | 方法被忽略 | 根據 Server 類型選擇正確的方法簽名 |
| 未處理 null 參數 | NullPointerException | 使用 Optional 或預設值 |
| 長時間阻塞同步方法 | 超時或效能問題 | 使用非同步或回報進度 |
| 回傳過大的資料 | 記憶體問題 | 實作分頁或串流 |

---

## 參考來源

| 優先級 | 來源類型 | 名稱 | 說明 | 連結 |
|--------|----------|------|------|------|
| 🥇 | 官方文件 | Spring AI MCP Server Boot Starter | Server Boot Starter 完整文件 | [Spring.io](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) |
| 🥇 | 官方文件 | Spring AI MCP Overview | MCP 概述 | [Spring.io](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) |
| 🥇 | 官方文件 | Getting Started with MCP | 入門指南 | [Spring.io](https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html) |
| 🥇 | 官方文件 | MCP Server Annotations | 註解參考 | [Spring.io](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html) |
| 🥇 | 官方部落格 | Spring AI 2.0.0-M1 Release | 版本發布公告 | [Spring.io](https://spring.io/blog/2025/12/11/spring-ai-2-0-0-M1-available-now/) |
| 🥇 | 官方 GitHub | Spring AI Examples | 官方範例專案 | [GitHub](https://github.com/spring-projects/spring-ai-examples/tree/main/model-context-protocol) |
| 4️⃣ | 社群 | Spring AI MCP Server Guide | 社群教學 | [skywork.ai](https://skywork.ai/skypage/en/spring-ai-mcp-server-deep-dive/1981177808883208192) |

---

## 決策記錄

| 日期 | 決策項目 | 選擇 | 原因 |
|------|----------|------|------|
| 2026-01-19 | Spring AI 版本 | 2.0.0-M1 | 最新 milestone，支援最新 MCP 協議 |
| 2026-01-19 | Java 版本 | 21+ | Spring AI 2.x 需求 |
| 2026-01-19 | 傳輸協議 | Streamable-HTTP | 推薦用於生產環境，替代 SSE |
