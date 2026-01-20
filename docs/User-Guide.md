# DocMCP Server 使用手冊

> **適用對象**：一般使用者（非工程師）
> **版本**：1.0.0
> **最後更新**：2026-01-20

---

## 目錄

1. [什麼是 DocMCP Server？](#1-什麼是-docmcp-server)
2. [快速入門](#2-快速入門)
3. [新增文件庫教學：以 Spring Boot 4.0.1 為例](#3-新增文件庫教學以-spring-boot-401-為例)
4. [功能介紹](#4-功能介紹)
5. [常見問題](#5-常見問題)

---

## 1. 什麼是 DocMCP Server？

DocMCP Server 是一個**文件管理伺服器**，它可以：

- 從 GitHub 自動抓取技術文件
- 將文件建立索引，方便搜尋
- 提供給 AI 編程助手（如 Claude、Cursor）使用

**簡單來說**：您只需要告訴系統「我想要 Spring Boot 4.0.1 的文件」，系統就會自動幫您抓取、整理好，讓 AI 助手可以查閱這些文件來幫助您寫程式。

---

## 2. 快速入門

### 2.1 打開 DocMCP Server

在瀏覽器中輸入伺服器網址（例如：`http://localhost:8080`），您會看到主畫面：

```
┌─────────────────────────────────────────────────────────────┐
│  DocMCP                                                      │
├──────────┬──────────────────────────────────────────────────┤
│          │                                                   │
│ Dashboard│     Libraries: 3    Documents: 256    Chunks: 1024│
│          │                                                   │
│ Libraries│  ┌─────────────────────────────────────────────┐ │
│          │  │ Recent Sync Activity                        │ │
│ Search   │  │ ...                                         │ │
│          │  └─────────────────────────────────────────────┘ │
│ Documents│                                                   │
│          │  [+ Add Library]        [Test Search]            │
│ Settings │                                                   │
│          │                                                   │
└──────────┴──────────────────────────────────────────────────┘
```

### 2.2 主要功能導覽

| 選單項目 | 說明 |
|----------|------|
| **Dashboard** | 首頁，顯示統計數據和最近同步活動 |
| **Libraries** | 管理文件庫（新增、編輯、刪除） |
| **Search** | 搜尋已匯入的文件內容 |
| **Documents** | 瀏覽所有已匯入的文件 |
| **Settings** | 系統設定（功能開關狀態） |

---

## 3. 新增文件庫教學：以 Spring Boot 4.0.1 為例

### 步驟概覽

```
┌─────────────────────────────────────────────────────────────┐
│                    新增文件庫流程                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   步驟 1          步驟 2           步驟 3          步驟 4    │
│  ┌──────┐       ┌──────┐        ┌──────┐        ┌──────┐   │
│  │建立  │  ──▶  │填寫  │  ──▶   │觸發  │  ──▶   │完成  │   │
│  │文件庫│       │資訊  │        │同步  │        │確認  │   │
│  └──────┘       └──────┘        └──────┘        └──────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### 步驟 1：進入「新增文件庫」頁面

1. 點擊左側選單的 **Libraries**
2. 點擊右上角的 **Add Library** 按鈕

或者，在 Dashboard 頁面直接點擊 **Add Library** 卡片。

---

### 步驟 2：填寫文件庫資訊

在「Add New Library」表單中，填寫以下資訊：

#### 欄位說明

| 欄位 | 必填 | 說明 | 範例 |
|------|------|------|------|
| **Name** | ✅ | 唯一識別碼，只能用小寫英文、數字、連字號 | `spring-boot` |
| **Display Name** | ✅ | 顯示名稱，可以用中文 | `Spring Boot` |
| **Description** | ❌ | 簡短描述這個文件庫 | `Spring Boot 官方文件` |
| **Source Type** | ✅ | 來源類型 | `GITHUB` |
| **Source URL** | ❌ | GitHub 儲存庫網址 | 見下方 |
| **Category** | ❌ | 分類 | `backend` |
| **Tags** | ❌ | 標籤，用逗號分隔 | `java, framework, spring` |

#### Spring Boot 4.0.1 填寫範例

```
Name:         spring-boot
Display Name: Spring Boot
Description:  Spring Boot 官方參考文件
Source Type:  GITHUB
Source URL:   https://github.com/spring-projects/spring-boot
Category:     backend
Tags:         java, framework, spring, web
```

> **提示**：Source URL 是 Spring Boot 的 GitHub 儲存庫網址。系統會自動從這個儲存庫抓取文件。

---

### 步驟 3：儲存文件庫

填寫完成後，點擊 **Create Library** 按鈕。

系統會建立文件庫，並自動跳轉到文件庫詳情頁面。

---

### 步驟 4：同步文件（重要！）

建立文件庫後，還需要**同步文件**才會真正抓取內容。

#### 4.1 點擊「Sync Version」按鈕

在文件庫詳情頁面，您會看到：

```
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot                                                  │
│ spring-boot                                                  │
├─────────────────────────────────────────────────────────────┤
│ Description: Spring Boot 官方參考文件                        │
│ Source Type: GITHUB                                          │
│ Source URL: https://github.com/spring-projects/spring-boot  │
├─────────────────────────────────────────────────────────────┤
│ Versions                              [Sync Version]         │
│                                                              │
│ No versions yet                                              │
│ Click "Sync Version" to add a new version                   │
└─────────────────────────────────────────────────────────────┘
```

點擊右上角的 **Sync Version** 按鈕。

#### 4.2 輸入版本號

在彈出的對話框中，輸入您想要同步的版本號：

```
┌─────────────────────────────────┐
│ Sync Version                    │
├─────────────────────────────────┤
│ Version:  [4.0.1          ]     │
│                                 │
│           [Cancel] [Sync]       │
└─────────────────────────────────┘
```

輸入 `4.0.1`，然後點擊 **Sync** 按鈕。

> **說明**：系統會自動在版本號前面加上 `v`，所以會抓取 GitHub 上的 `v4.0.1` 標籤。

#### 4.3 等待同步完成

同步開始後，您會看到狀態變為 **Running**：

```
┌────────────────────────────────────────────────────────────────────┐
│ Versions                                          [Sync Version]  │
├──────────┬──────────┬───────────┬─────────────┬──────────────────┤
│ Version  │ Status   │ Docs Path │ Last Sync   │ Actions          │
├──────────┼──────────┼───────────┼─────────────┼──────────────────┤
│ 4.0.1    │ Active   │ docs      │ Running     │ Browse Docs Sync │
│          │          │           │ 01-20 10:30 │                  │
└──────────┴──────────┴───────────┴─────────────┴──────────────────┘
```

等待幾分鐘（視文件數量而定），狀態會變為 **Success**。

---

### 步驟 5：確認同步結果

同步完成後：

1. **Last Sync** 會顯示 **Success** 和時間
2. 點擊 **Browse Docs** 可以瀏覽已匯入的文件
3. 到 **Dashboard** 可以看到 Documents 和 Chunks 數量增加

恭喜！您已經成功新增 Spring Boot 4.0.1 的文件庫！

---

## 4. 功能介紹

### 4.1 搜尋文件

1. 點擊左側選單的 **Search**
2. 在搜尋框輸入關鍵字（例如：`@RestController`）
3. 點擊 **Search** 按鈕或按 Enter

搜尋結果會顯示：
- 文件標題
- 相關內容片段
- 所屬文件庫和版本

### 4.2 瀏覽文件

1. 點擊左側選單的 **Documents**
2. 可以使用下拉選單篩選特定版本
3. 點擊文件標題查看完整內容

### 4.3 編輯文件庫

1. 點擊 **Libraries** 進入文件庫列表
2. 點擊要編輯的文件庫名稱
3. 在詳情頁面點擊 **Edit** 按鈕
4. 修改資訊後點擊 **Update Library**

### 4.4 同步更多版本

如果要新增其他版本（例如 3.3.5）：

1. 進入文件庫詳情頁面
2. 點擊 **Sync Version**
3. 輸入版本號 `3.3.5`
4. 點擊 **Sync**

每個版本會獨立管理，互不影響。

### 4.5 刪除文件庫

1. 進入文件庫詳情頁面
2. 點擊右上角的 **Delete** 按鈕
3. 確認刪除

> **警告**：刪除文件庫會同時刪除所有版本和已匯入的文件，此操作無法復原！

---

## 5. 常見問題

### Q1: 同步失敗怎麼辦？

**可能原因**：
- GitHub 儲存庫網址錯誤
- 版本號不存在（例如輸入 `4.0.2` 但實際只有 `4.0.1`）
- 網路連線問題

**解決方法**：
1. 確認 Source URL 是正確的 GitHub 網址
2. 到 GitHub 確認該版本標籤存在（格式為 `v版本號`）
3. 重新點擊 **Sync** 按鈕再試一次

### Q2: 如何找到正確的 GitHub 網址？

1. 在瀏覽器開啟 [github.com](https://github.com)
2. 搜尋您要的專案（例如 `spring-boot`）
3. 進入專案頁面後，複製網址列的網址

範例：
```
https://github.com/spring-projects/spring-boot
```

### Q3: 如何知道有哪些版本可以同步？

1. 進入 GitHub 儲存庫頁面
2. 點擊 **Releases** 或 **Tags**
3. 查看可用的版本列表

版本號通常以 `v` 開頭（如 `v4.0.1`），輸入時**不需要**加 `v`。

### Q4: 同步需要多久時間？

取決於文件數量和大小：
- 小型專案：1-2 分鐘
- 中型專案（如 Spring Boot）：3-5 分鐘
- 大型專案：可能需要 10 分鐘以上

同步期間可以做其他事情，完成後狀態會自動更新。

### Q5: 可以同步私有儲存庫嗎？

目前版本僅支援公開的 GitHub 儲存庫。

### Q6: 文件格式有限制嗎？

系統支援以下格式的文件：
- Markdown (`.md`)
- AsciiDoc (`.adoc`)
- HTML (`.html`)

其他格式的文件會被略過。

---

## 附錄 A：支援的文件來源

| 來源類型 | 說明 |
|----------|------|
| **GITHUB** | GitHub 公開儲存庫 |
| **GITLAB** | GitLab 公開儲存庫（規劃中） |
| **MANUAL** | 手動上傳（規劃中） |

---

## 附錄 B：常用 GitHub 文件庫網址

| 專案名稱 | GitHub 網址 |
|----------|-------------|
| Spring Boot | `https://github.com/spring-projects/spring-boot` |
| Spring Framework | `https://github.com/spring-projects/spring-framework` |
| React | `https://github.com/facebook/react` |
| Vue.js | `https://github.com/vuejs/vue` |
| TypeScript | `https://github.com/microsoft/TypeScript` |
| PostgreSQL Docs | `https://github.com/postgres/postgres` |

---

## 附錄 C：快速操作檢查表

### 新增文件庫檢查表

- [ ] 確認 GitHub 儲存庫網址正確
- [ ] 確認版本號存在於 GitHub Tags/Releases
- [ ] Name 欄位只使用小寫英文、數字、連字號
- [ ] 點擊 Create Library 後，記得點擊 Sync Version
- [ ] 等待同步完成（狀態顯示 Success）

### 疑難排解檢查表

- [ ] 檢查網路連線是否正常
- [ ] 確認 GitHub 網址格式：`https://github.com/擁有者/專案名稱`
- [ ] 確認版本標籤存在：到 GitHub 的 Tags 頁面確認
- [ ] 查看 Dashboard 的 Recent Sync Activity 是否有錯誤訊息

---

**如有其他問題，請聯繫系統管理員。**
