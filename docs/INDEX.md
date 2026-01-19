# Documentation MCP Server 技術文件索引

> 最後更新：2026-01-19

## 概述

本專案是一個現代化的 **Documentation MCP Server**，使用 Spring AI 2.0.0-M1 開發，提供通用的文件服務給 AI 助手。不僅限於 Spring 文件，而是設計為可支援任何技術文件的 MCP Server。

---

## 分類說明

| 分類 | 說明 | 路徑 |
|------|------|------|
| **mcp** | Model Context Protocol 相關文件 | `docs/mcp/` |
| **research** | 技術研究與分析 | `docs/research/` |

---

## 文件目錄

### 專案文件

| 文件 | 說明 |
|------|------|
| [PRD - 產品需求文件](PRD.md) | DocMCP Server 的完整產品需求規格，包含技術架構、功能需求、開發計畫 |

### MCP (Model Context Protocol)

| 文件 | 難度 | 說明 |
|------|------|------|
| [MCP 規格研究](mcp/mcp-specification.md) | 中級 | MCP 協議規格、架構、Tools/Resources/Prompts、安全考量與最佳實踐 |
| [Spring AI MCP 開發指南](mcp/spring-ai-mcp-development.md) | 中級 | 使用 Spring AI 2.0.0-M1 開發 MCP Server 的完整指南 |
| [Documentation MCP Server 設計指南](mcp/documentation-mcp-server-design.md) | 進階 | 現代化 Documentation MCP Server 的架構設計與實作建議 |

### Research (技術研究)

| 文件 | 說明 |
|------|------|
| [spring-documentation-mcp-server 深度分析](research/spring-documentation-mcp-server-analysis.md) | 46 個 MCP 工具、混合搜尋架構、Flavors 系統、資料庫設計的完整分析 |

---

## 建議學習順序

### 新手路線

```
1. MCP 規格研究
   ├── 了解 MCP 是什麼
   ├── 理解 Tools, Resources, Prompts 概念
   └── 認識安全考量

2. Spring AI MCP 開發指南
   ├── 環境配置
   ├── 核心註解使用
   └── 快速開始範例

3. Documentation MCP Server 設計指南
   ├── 參考專案分析
   ├── 架構設計
   └── 實作路線圖
```

### 進階路線

如果您已有 MCP 基礎：

```
1. Spring AI MCP 開發指南 → 進階功能
2. Documentation MCP Server 設計指南 → 實作建議
```

---

## 快速連結

### 官方資源

- [MCP 官方規格](https://modelcontextprotocol.io/specification/2025-11-25)
- [Spring AI MCP 文件](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
- [MCP GitHub](https://github.com/modelcontextprotocol)

### 參考專案

- [Context7 (Upstash)](https://github.com/upstash/context7)
- [spring-documentation-mcp-server](https://github.com/andrlange/spring-documentation-mcp-server)

---

## 技術堆疊

本專案使用以下技術（基於 `build.gradle` 配置）：

| 類別 | 技術 | 版本 |
|------|------|------|
| 語言 | Java | 25 |
| 框架 | Spring Boot | 4.0.1 |
| AI 框架 | Spring AI | 2.0.0-M1 |
| MCP 傳輸 | WebMVC (Streamable-HTTP) | 2025-11-25 |
| 向量儲存 | PostgreSQL + pgvector | - |
| 模板引擎 | Thymeleaf | - |
| 建構工具 | Gradle | - |
| 開發環境 | Docker Compose | - |
| 測試框架 | Testcontainers | - |

---

## 更新記錄

| 日期 | 更新內容 |
|------|----------|
| 2026-01-19 | PRD 1.3.0：新增繁體中文詳細註解說明 |
| 2026-01-19 | PRD 1.2.0：改用 Gemini text-embedding-004，非 starter 手動配置 |
| 2026-01-19 | PRD 1.1.0：整合 spring-documentation-mcp-server 優秀設計 |
| 2026-01-19 | 新增 spring-documentation-mcp-server 深度分析 |
| 2026-01-19 | 新增 PRD 產品需求文件，修正技術堆疊為 Gradle |
| 2026-01-19 | 初始化文件結構，完成 MCP 研究文件 |

---

## 貢獻指南

### 新增文件

1. 根據主題選擇適當的分類目錄
2. 使用 `lowercase-with-hyphen.md` 命名
3. 遵循文件模板格式
4. 更新本索引文件

### 文件模板

所有文件應包含以下區塊：

```markdown
# 標題

> 最後更新：YYYY-MM-DD
> 難度：入門 / 中級 / 進階
> 前置知識：[需要先了解什麼]

## 目錄
## 簡介
## 核心概念
## 快速開始
## 最佳實踐
## 參考來源
## 決策記錄
```
