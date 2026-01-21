# MCP 整合教學指南

> 將 DocMCP Server 連接到您的 AI 編程助手
>
> 最後更新：2026-01-21

---

## 目錄

1. [概述](#概述)
2. [前置需求](#前置需求)
3. [Claude Code 整合](#claude-code-整合)
4. [VS Code + GitHub Copilot 整合](#vs-code--github-copilot-整合)
5. [Cursor 整合](#cursor-整合)
6. [驗證連線](#驗證連線)
7. [常見問題](#常見問題)

---

## 概述

DocMCP Server 提供 **Stateless HTTP** 傳輸的 MCP（Model Context Protocol）端點，讓 AI 編程助手可以查詢技術文件。

### MCP 端點資訊

| 項目 | 值 |
|------|-----|
| **端點 URL** | `http://your-server:8080/mcp` |
| **傳輸協議** | HTTP (Stateless) |
| **認證方式** | Bearer Token（若啟用 API Key 認證） |

### 提供的 MCP 工具

DocMCP Server 提供以下工具供 AI 助手使用：

| 工具名稱 | 說明 |
|----------|------|
| `list_libraries` | 列出所有可用的文件庫 |
| `resolve_library` | 解析文件庫資訊與可用版本 |
| `search_docs` | 全文搜尋文件內容 |
| `semantic_search` | 語意搜尋（向量相似度） |
| `get_doc_content` | 取得特定文件的完整內容 |
| `get_code_examples` | 取得程式碼範例 |

---

## 前置需求

### 1. 確認 DocMCP Server 已啟動

```bash
# 檢查伺服器是否運行中
curl http://localhost:8080/actuator/health
```

預期回應：
```json
{"status":"UP"}
```

### 2. 取得 API Key（若有啟用認證）

如果 DocMCP Server 啟用了 API Key 認證，您需要先取得 API Key：

1. 開啟 DocMCP Server Web UI（`http://localhost:8080`）
2. 進入 **Settings** 頁面
3. 在 API Keys 區塊建立新的 Key

---

## Claude Code 整合

[Claude Code](https://claude.ai/code) 是 Anthropic 官方的 CLI 工具，支援 MCP 協議。

### 方法一：使用 CLI 指令（推薦）

#### 基本設定（無認證）

```bash
claude mcp add --transport http docmcp http://localhost:8080/mcp
```

#### 帶認證的設定

```bash
claude mcp add --transport http docmcp http://localhost:8080/mcp \
  --header "Authorization: Bearer your-api-key-here"
```

#### 設定作用域

```bash
# 僅當前專案使用（預設）
claude mcp add --scope local --transport http docmcp http://localhost:8080/mcp

# 團隊共享（寫入 .mcp.json）
claude mcp add --scope project --transport http docmcp http://localhost:8080/mcp

# 跨專案使用（個人設定）
claude mcp add --scope user --transport http docmcp http://localhost:8080/mcp
```

### 方法二：手動編輯設定檔

#### 專案設定（`.mcp.json`）

在專案根目錄建立 `.mcp.json` 檔案：

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

#### 帶認證的設定

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer your-api-key-here"
      }
    }
  }
}
```

#### 使用環境變數（推薦用於敏感資訊）

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "${DOCMCP_URL:-http://localhost:8080}/mcp",
      "headers": {
        "Authorization": "Bearer ${DOCMCP_API_KEY}"
      }
    }
  }
}
```

然後設定環境變數：

```bash
export DOCMCP_URL="http://your-server:8080"
export DOCMCP_API_KEY="your-api-key-here"
```

#### 使用者全域設定（`~/.claude.json`）

編輯 `~/.claude.json` 檔案，加入 MCP 設定：

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### 驗證 Claude Code 設定

```bash
# 列出所有已設定的 MCP 伺服器
claude mcp list

# 查看特定伺服器詳情
claude mcp get docmcp

# 在 Claude Code 內檢查狀態
# 啟動 Claude Code 後輸入：
/mcp
```

### 移除設定

```bash
claude mcp remove docmcp
```

---

## VS Code + GitHub Copilot 整合

VS Code 1.99 以上版本支援 MCP 伺服器整合 GitHub Copilot。

### 前置需求

- VS Code 版本 1.99 或更新
- GitHub Copilot 擴充套件已安裝
- （企業用戶）確認組織已啟用「MCP servers in Copilot」政策

### 方法一：使用 VS Code 設定（推薦）

#### 步驟 1：開啟設定

按 `Cmd+Shift+P`（Mac）或 `Ctrl+Shift+P`（Windows/Linux），輸入：

```
Preferences: Open User Settings (JSON)
```

#### 步驟 2：加入 MCP 設定

在 `settings.json` 中加入：

```json
{
  "github.copilot.chat.mcp.servers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

#### 帶認證的設定

```json
{
  "github.copilot.chat.mcp.servers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer your-api-key-here"
      }
    }
  }
}
```

#### 使用輸入變數（安全輸入 API Key）

```json
{
  "github.copilot.chat.mcp.servers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer ${input:docmcp-api-key}"
      }
    }
  }
}
```

VS Code 會在連線時提示您輸入 API Key。

### 方法二：專案設定檔（`.vscode/mcp.json`）

在專案的 `.vscode` 目錄建立 `mcp.json` 檔案：

```json
{
  "servers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### 驗證 VS Code 設定

1. 開啟 Copilot Chat（`Cmd+Shift+I` 或 `Ctrl+Shift+I`）
2. 輸入 `@docmcp` 來呼叫 MCP 工具
3. 或直接詢問：「使用 docmcp 搜尋 Spring Boot 的 @RestController 說明」

---

## Cursor 整合

[Cursor](https://cursor.sh) 是一個 AI 驅動的程式碼編輯器，同樣支援 MCP 協議。

### 設定方式

#### 步驟 1：開啟 Cursor 設定

1. 點擊右上角齒輪圖示或按 `Cmd+,`
2. 搜尋「MCP」或進入 Features > MCP Servers

#### 步驟 2：新增 MCP 伺服器

點擊「+ Add new MCP server」，填入以下資訊：

| 欄位 | 值 |
|------|-----|
| Name | `docmcp` |
| Type | `http` |
| URL | `http://localhost:8080/mcp` |

#### 或手動編輯設定檔

Cursor 的 MCP 設定檔位於：

- **macOS**：`~/.cursor/mcp.json`
- **Windows**：`%APPDATA%\Cursor\mcp.json`
- **Linux**：`~/.config/cursor/mcp.json`

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### 專案設定（`.cursor/mcp.json`）

在專案根目錄建立 `.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

---

## 驗證連線

### 1. 使用 curl 測試端點

```bash
# 測試 MCP 端點是否回應
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

預期回應會包含工具列表：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {"name": "list_libraries", ...},
      {"name": "search_docs", ...},
      ...
    ]
  }
}
```

### 2. 在 AI 助手中測試

設定完成後，在您的 AI 編程助手中嘗試以下問題：

- 「列出所有可用的文件庫」
- 「搜尋 Spring Boot 的 @RestController 用法」
- 「查詢 Spring Boot 4.0.1 的 WebClient 設定方式」

如果 AI 助手能夠回應並引用 DocMCP 的文件內容，表示整合成功！

---

## 常見問題

### Q1: 連線被拒絕（Connection Refused）

**可能原因**：
- DocMCP Server 未啟動
- 防火牆阻擋連線
- URL 錯誤

**解決方法**：
1. 確認伺服器已啟動：`curl http://localhost:8080/actuator/health`
2. 檢查防火牆設定
3. 確認 URL 格式正確（注意 `http` vs `https`）

### Q2: 認證失敗（401 Unauthorized）

**可能原因**：
- API Key 錯誤或過期
- Header 格式錯誤

**解決方法**：
1. 確認 API Key 正確
2. 確認 Header 格式：`Authorization: Bearer <your-key>`
3. 重新產生 API Key

### Q3: Claude Code 顯示「MCP server not responding」

**解決方法**：
1. 執行 `claude mcp get docmcp` 檢查設定
2. 設定較長的逾時時間：`MCP_TIMEOUT=30000 claude`
3. 重新啟動 Claude Code

### Q4: VS Code 找不到 MCP 工具

**解決方法**：
1. 確認 VS Code 版本 >= 1.99
2. 重新載入視窗（`Cmd+Shift+P` > `Developer: Reload Window`）
3. 檢查設定檔語法是否正確

### Q5: 工具呼叫沒有回傳結果

**可能原因**：
- 文件庫尚未同步
- 搜尋關鍵字無匹配結果

**解決方法**：
1. 開啟 DocMCP Web UI 確認有已同步的文件庫
2. 嘗試更廣泛的搜尋關鍵字
3. 先使用 `list_libraries` 確認可用的文件庫

---

## 附錄：完整設定範例

### Claude Code（`.mcp.json`）

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "${DOCMCP_URL:-http://localhost:8080}/mcp",
      "headers": {
        "Authorization": "Bearer ${DOCMCP_API_KEY}"
      }
    }
  }
}
```

### VS Code（`settings.json`）

```json
{
  "github.copilot.chat.mcp.servers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "Authorization": "Bearer ${input:docmcp-api-key}"
      }
    }
  }
}
```

### Cursor（`~/.cursor/mcp.json`）

```json
{
  "mcpServers": {
    "docmcp": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

---

## 參考資源

- [Claude Code MCP 官方文件](https://code.claude.com/docs/en/mcp)
- [VS Code MCP Servers 設定指南](https://code.visualstudio.com/docs/copilot/customization/mcp-servers)
- [GitHub Copilot MCP 整合說明](https://docs.github.com/copilot/customizing-copilot/using-model-context-protocol/extending-copilot-chat-with-mcp)
- [MCP 官方規格](https://modelcontextprotocol.io/specification/2025-11-25)

---

**如有其他整合問題，歡迎回報 Issue！**
