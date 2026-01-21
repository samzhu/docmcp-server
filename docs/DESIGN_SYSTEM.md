# DocMCP Design System

> 基於 Apple WWDC 2025 Liquid Glass 設計語言的混合模式設計系統

---

## 目錄

1. [設計理念](#設計理念)
2. [核心原則](#核心原則)
3. [色彩系統](#色彩系統)
4. [字體系統](#字體系統)
5. [間距系統](#間距系統)
6. [圓角系統](#圓角系統)
7. [陰影系統](#陰影系統)
8. [動畫系統](#動畫系統)
9. [元件規範](#元件規範)
10. [響應式設計](#響應式設計)
11. [CSS 變數速查表](#css-變數速查表)
12. [常用工具類別速查表](#常用工具類別速查表)
13. [新增頁面檢查清單](#新增頁面檢查清單)

---

## 設計理念

本設計系統採用 **Apple 混合模式設計**，結合 Liquid Glass 效果與實心內容層：

```
┌─────────────────────────────────────────────────────────┐
│  背景層：彩色漸層（紫/粉漸層）                            │
├────────────┬────────────────────────────────────────────┤
│            │                                            │
│  Sidebar   │     主內容區                                │
│  (深色     │     (淺色覆蓋層 92% 透明度)                 │
│  Liquid    │                                            │
│  Glass)    │     ┌──────────────────────────────────┐  │
│            │     │  內容卡片                         │  │
│  背景漸層  │     │  - 實心白色背景 #ffffff           │  │
│  透出      │     │  - 微妙陰影                       │  │
│            │     └──────────────────────────────────┘  │
│            │                                            │
└────────────┴────────────────────────────────────────────┘
```

### 層次架構

| 層級 | 元素 | 效果 | 用途 |
|------|------|------|------|
| **L0 背景層** | body | 彩色漸層 + 微妙紋理 | 提供視覺深度 |
| **L1 導航層** | Sidebar | 深色 Liquid Glass | 導航元素，透出背景 |
| **L2 覆蓋層** | .app-main::before | 淺色半透明覆蓋 | 降低背景對比，提升可讀性 |
| **L3 內容層** | 卡片、表單 | 實心白色 + 陰影 | 主要內容區域 |

---

## 核心原則

### 1. Liquid Glass 只用於導航

```css
/* ✅ 正確：Sidebar 使用 Liquid Glass */
.sidebar {
    background: rgba(30, 30, 32, 0.88);
    backdrop-filter: blur(60px) saturate(180%);
}

/* ❌ 錯誤：內容卡片不應使用 Liquid Glass */
.card {
    background: rgba(255, 255, 255, 0.45);  /* 避免 */
    backdrop-filter: blur(24px);            /* 避免 */
}
```

### 2. 內容區使用實心背景

```css
/* ✅ 正確：內容卡片使用實心白色 */
.card {
    background: #ffffff;
    border: 1px solid rgba(0, 0, 0, 0.06);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04),
                0 4px 16px rgba(0, 0, 0, 0.06);
}
```

### 3. 深度透過陰影表達

```css
/* 基礎陰影 */
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04),
            0 4px 16px rgba(0, 0, 0, 0.06);

/* Hover 增強陰影 */
box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08),
            0 8px 24px rgba(0, 0, 0, 0.1);
```

---

## 色彩系統

### 背景色

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--color-bg-gradient` | `linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)` | 頁面背景漸層 |
| `--color-bg-overlay` | `rgba(245, 245, 247, 0.92)` | 主內容區覆蓋層 |

### 文字色

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--color-text-primary` | `#1d1d1f` | 主要文字 |
| `--color-text-secondary` | `#6e6e73` | 次要文字、標籤 |
| `--color-text-tertiary` | `#aeaeb2` | 輔助文字、placeholder |
| `--color-text-inverse` | `#ffffff` | 深色背景上的文字 |

### 強調色

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--color-accent` | `#007aff` | Apple 藍，連結、按鈕 |
| `--color-accent-hover` | `#0a84ff` | Hover 狀態 |
| `--color-accent-gradient` | `linear-gradient(135deg, #007aff 0%, #5856d6 100%)` | 主要按鈕漸層 |
| `--color-accent-light` | `rgba(0, 122, 255, 0.12)` | 輕量背景色 |

### 狀態色

| 狀態 | 前景色 | 背景色 | 用途 |
|------|--------|--------|------|
| Success | `#30d158` | `rgba(48, 209, 88, 0.15)` | 成功、啟用 |
| Warning | `#ff9f0a` | `rgba(255, 159, 10, 0.15)` | 警告、注意 |
| Error | `#ff453a` | `rgba(255, 69, 58, 0.15)` | 錯誤、危險 |
| Info | `#64d2ff` | `rgba(100, 210, 255, 0.15)` | 資訊提示 |

### 使用範例

```html
<!-- 成功狀態標籤 -->
<span class="badge badge-success">Enabled</span>

<!-- 警告狀態標籤 -->
<span class="badge badge-warning">Pending</span>

<!-- 錯誤狀態標籤 -->
<span class="badge badge-danger">Failed</span>
```

---

## 字體系統

### 字體家族

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--font-display` | SF Pro Display, system-ui | 標題、大字 |
| `--font-text` | SF Pro Text, system-ui | 內文、按鈕 |
| `--font-mono` | SF Mono, JetBrains Mono | 程式碼、版本號 |

### 字體大小

| 類別 | 大小 | 用途 |
|------|------|------|
| `.text-xs` | 0.75rem (12px) | 輔助標籤、日期 |
| `.text-sm` | 0.875rem (14px) | 次要文字、按鈕 |
| `.text-base` | 1rem (16px) | 內文 |
| `.text-lg` | 1.125rem (18px) | 副標題 |
| `.text-xl` | 1.25rem (20px) | 小標題 |
| `.text-2xl` | 1.5rem (24px) | 主標題 |

### 字重

| 類別 | 值 | 用途 |
|------|-----|------|
| `.font-medium` | 500 | 強調文字 |
| `.font-semibold` | 600 | 按鈕、標籤 |
| `.font-bold` | 700 | 數字、標題 |

### 使用範例

```html
<!-- 頁面標題 -->
<h1 class="text-2xl font-semibold text-primary">Dashboard</h1>

<!-- 統計數字 -->
<span class="text-2xl font-bold text-primary">2,202</span>

<!-- 程式碼/版本 -->
<code class="font-mono text-sm">v3.4.1</code>
```

---

## 間距系統

### 間距變數

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--spacing-xs` | 0.25rem (4px) | 元素內微間距 |
| `--spacing-sm` | 0.5rem (8px) | 緊湊間距 |
| `--spacing-md` | 1rem (16px) | 標準間距 |
| `--spacing-lg` | 1.5rem (24px) | 寬鬆間距 |
| `--spacing-xl` | 2rem (32px) | 區塊間距 |
| `--spacing-2xl` | 3rem (48px) | 大區塊間距 |

### 工具類別

```css
/* Padding */
.p-2  → padding: 0.5rem (8px)
.p-4  → padding: 1rem (16px)
.p-6  → padding: 1.5rem (24px)
.px-4 → padding-left/right: 1rem
.py-2 → padding-top/bottom: 0.5rem

/* Margin - Top */
.mt-1 → margin-top: 0.25rem (4px)
.mt-2 → margin-top: 0.5rem (8px)
.mt-3 → margin-top: 0.75rem (12px)
.mt-4 → margin-top: 1rem (16px)
.mt-6 → margin-top: 1.5rem (24px)
.mt-8 → margin-top: 2rem (32px)    /* 表單按鈕區塊間距 */

/* Margin - Bottom */
.mb-2 → margin-bottom: 0.5rem (8px)
.mb-4 → margin-bottom: 1rem (16px)
.mb-6 → margin-bottom: 1.5rem (24px)
.mb-8 → margin-bottom: 2rem (32px)

/* Margin - Left/Right */
.ml-2 → margin-left: 0.5rem
.ml-4 → margin-left: 1rem
.mr-2 → margin-right: 0.5rem

/* Gap (Flexbox/Grid) */
.gap-1 → gap: 0.25rem (4px)
.gap-2 → gap: 0.5rem (8px)
.gap-3 → gap: 0.75rem (12px)   /* 通知框 icon 與文字間距 */
.gap-4 → gap: 1rem (16px)
.gap-6 → gap: 1.5rem (24px)

/* Space (子元素間距) */
.space-x-2 > * + * → margin-left: 0.5rem
.space-x-4 > * + * → margin-left: 1rem   /* 按鈕群組間距 */
.space-y-4 > * + * → margin-top: 1rem
.space-y-6 > * + * → margin-top: 1.5rem  /* 表單欄位間距 */
```

### 尺寸工具類別

```css
/* 寬度 */
.w-4     → width: 1rem (16px)
.w-5     → width: 1.25rem (20px)   /* icon 標準尺寸 */
.w-6     → width: 1.5rem (24px)
.w-full  → width: 100%

/* 高度 */
.h-4     → height: 1rem (16px)
.h-5     → height: 1.25rem (20px)  /* icon 標準尺寸 */
.h-6     → height: 1.5rem (24px)

/* 最大寬度 */
.max-w-xs  → max-width: 20rem (320px)
.max-w-2xl → max-width: 42rem (672px)  /* 表單卡片最大寬度 */
```

---

## 圓角系統

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--radius-sm` | 12px | 按鈕、輸入框、小卡片 |
| `--radius-md` | 16px | 搜尋框、中型卡片 |
| `--radius-lg` | 22px | 內容卡片 |
| `--radius-xl` | 28px | Modal 彈窗 |
| `--radius-full` | 9999px | 圓形按鈕、頭像 |

### 使用範例

```html
<!-- 按鈕 -->
<button class="btn btn-primary">Submit</button>  <!-- radius-sm: 12px -->

<!-- 卡片 -->
<div class="card">...</div>  <!-- radius-lg: 22px -->

<!-- 頭像 -->
<div class="header-avatar">A</div>  <!-- radius-full -->
```

---

## 陰影系統

### 陰影層級

| 用途 | CSS 值 |
|------|--------|
| **基礎陰影** | `0 2px 8px rgba(0,0,0,0.04), 0 4px 16px rgba(0,0,0,0.06)` |
| **Hover 陰影** | `0 4px 12px rgba(0,0,0,0.08), 0 8px 24px rgba(0,0,0,0.1)` |
| **Modal 陰影** | `0 8px 32px rgba(0,0,0,0.16), 0 16px 64px rgba(0,0,0,0.12)` |

### 按鈕特殊陰影

```css
/* 主要按鈕 - 帶色彩光暈 */
.btn-primary {
    box-shadow:
        0 2px 8px rgba(0, 122, 255, 0.3),
        0 4px 16px rgba(88, 86, 214, 0.2),
        inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* 危險按鈕 - 紅色光暈 */
.btn-danger {
    box-shadow:
        0 2px 8px rgba(255, 69, 58, 0.3),
        inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
```

---

## 動畫系統

### 過渡曲線

| 變數名稱 | 值 | 用途 |
|----------|-----|------|
| `--transition-fast` | 180ms cubic-bezier(0.25, 0.1, 0.25, 1) | Hover 效果 |
| `--transition-normal` | 280ms cubic-bezier(0.25, 0.1, 0.25, 1) | 狀態變化 |
| `--transition-slow` | 400ms cubic-bezier(0.25, 0.1, 0.25, 1) | 頁面過渡 |
| `--transition-bounce` | 500ms cubic-bezier(0.34, 1.56, 0.64, 1) | 彈性效果 |

### 內建動畫

```css
/* 進場動畫 */
.fade-in {
    animation: fadeInUp 0.5s forwards;
}

/* 通知滑入 */
.notification {
    animation: slideInRight 0.4s var(--transition-bounce);
}

/* 載入旋轉 */
.spinner {
    animation: spin 0.8s linear infinite;
}
```

### 交錯動畫

卡片元素會依序進場：

```css
.stats-grid > *:nth-child(1) { animation-delay: 0ms; }
.stats-grid > *:nth-child(2) { animation-delay: 60ms; }
.stats-grid > *:nth-child(3) { animation-delay: 120ms; }
```

---

## 元件規範

### 卡片 Card

用於包裝主要內容區塊。

```html
<div class="card">
    <h3>標題</h3>
    <p>內容...</p>
</div>
```

**樣式規格：**
- 背景：實心白色 `#ffffff`
- 邊框：`1px solid rgba(0, 0, 0, 0.06)`
- 圓角：`22px` (--radius-lg)
- 內距：`24px` (--spacing-lg)
- 陰影：基礎陰影
- Hover：上移 2px + 增強陰影

**變體：**
- `.card` / `.content-card` - 標準卡片（有 hover 效果）
- `.card-static` / `.glass-card-static` - 靜態卡片（無 hover）

---

### 統計卡片 Stat Card

用於顯示數字統計。

```html
<div class="stat-card">
    <div class="stat-icon blue">
        <svg>...</svg>
    </div>
    <div class="stat-content">
        <div class="stat-value">159</div>
        <div class="stat-label">Documents</div>
    </div>
</div>
```

**Icon 顏色變體：**
- `.stat-icon.blue` - 藍色（Libraries、Documents）
- `.stat-icon.green` - 綠色（成功狀態）
- `.stat-icon.yellow` - 黃色（Chunks、警告）

---

### 按鈕 Button

```html
<!-- 主要按鈕 -->
<button class="btn btn-primary">
    <svg>...</svg>
    Add Library
</button>

<!-- 次要按鈕 -->
<button class="btn btn-secondary">Cancel</button>

<!-- 危險按鈕 -->
<button class="btn btn-danger">Delete</button>
```

**樣式規格：**

| 類型 | 背景 | 文字色 | 特效 |
|------|------|--------|------|
| Primary | 藍紫漸層 | 白色 | 發光陰影 + 頂部高光 |
| Secondary | 淺灰 `rgba(0,0,0,0.04)` | 深灰 | 微陰影 |
| Danger | 紅色漸層 | 白色 | 紅色發光 |

---

### 表單元素 Form

```html
<label class="form-label">Library Name</label>
<input type="text" class="form-input" placeholder="Enter name...">

<select class="form-input">
    <option>Option 1</option>
</select>

<textarea class="form-input">...</textarea>
```

**樣式規格：**
- 背景：實心白色
- 邊框：`1px solid rgba(0, 0, 0, 0.12)`
- 圓角：`12px` (--radius-sm)
- Focus：藍色邊框 + 藍色光暈 `0 0 0 4px rgba(0,122,255,0.12)`

---

### 搜尋框 Search Box

```html
<div class="search-box">
    <svg><!-- 搜尋圖標 --></svg>
    <input type="text" placeholder="Search for documentation...">
</div>
```

---

### 狀態標籤 Badge

```html
<span class="badge badge-success">Enabled</span>
<span class="badge badge-warning">Pending</span>
<span class="badge badge-danger">Failed</span>
<span class="badge badge-info">New</span>
<span class="badge badge-secondary">Disabled</span>
```

---

### 表格 Table

```html
<table class="table">
    <thead>
        <tr>
            <th>VERSION ID</th>
            <th>STATUS</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>be85828b...</td>
            <td><span class="badge badge-success">Success</span></td>
        </tr>
    </tbody>
</table>
```

---

### Modal 彈窗

```html
<div class="modal-backdrop"></div>
<div class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h3>Modal Title</h3>
            <p>Description text</p>
        </div>
        <div class="modal-body">
            <!-- 內容 -->
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary">Cancel</button>
            <button class="btn btn-primary">Confirm</button>
        </div>
    </div>
</div>
```

---

### 功能卡片 Feature Card

用於 Settings 頁面的功能開關顯示。

```html
<div class="feature-card">
    <div class="feature-card-info">
        <svg>...</svg>
        <span>Web UI</span>
    </div>
    <span class="badge badge-success">Enabled</span>
</div>
```

---

### 快速操作卡片 Action Card

```html
<a href="/libraries/new" class="action-card">
    <div class="action-card-icon" style="background: var(--color-accent-light); color: var(--color-accent);">
        <svg>...</svg>
    </div>
    <div class="action-card-content">
        <h3>Add Library</h3>
        <p>Register a new documentation library</p>
    </div>
</a>
```

---

## 響應式設計

### 斷點

| 斷點 | 最大寬度 | 變化 |
|------|----------|------|
| Desktop | > 1024px | 側邊欄 260px |
| Tablet | 768px - 1023px | 側邊欄 220px |
| Mobile | < 768px | 側邊欄隱藏（滑出式） |

### 格線系統

```html
<!-- 統計卡片：3→2→1 欄 -->
<div class="stats-grid">
    <div class="stat-card">...</div>
    <div class="stat-card">...</div>
    <div class="stat-card">...</div>
</div>

<!-- 快速操作：2→1 欄 -->
<div class="action-cards-grid">
    <a class="action-card">...</a>
    <a class="action-card">...</a>
</div>

<!-- Library 卡片：自動填充 -->
<div class="library-cards-grid">
    <div class="card">...</div>
</div>
```

### 響應式工具類別

```css
/* 在 sm (640px+) 時變為 2 欄 */
.sm\:grid-cols-2

/* 在 md (768px+) 時變為 3 欄 */
.md\:grid-cols-3

/* 在 lg (1024px+) 時變為 4 欄 */
.lg\:grid-cols-4
```

---

## CSS 變數速查表

```css
:root {
    /* === 背景 === */
    --color-bg-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
    --color-bg-overlay: rgba(245, 245, 247, 0.92);

    /* === 文字 === */
    --color-text-primary: #1d1d1f;
    --color-text-secondary: #6e6e73;
    --color-text-tertiary: #aeaeb2;
    --color-text-inverse: #ffffff;

    /* === 強調色 === */
    --color-accent: #007aff;
    --color-accent-gradient: linear-gradient(135deg, #007aff 0%, #5856d6 100%);
    --color-accent-light: rgba(0, 122, 255, 0.12);

    /* === 狀態色 === */
    --color-success: #30d158;
    --color-success-bg: rgba(48, 209, 88, 0.15);
    --color-warning: #ff9f0a;
    --color-warning-bg: rgba(255, 159, 10, 0.15);
    --color-error: #ff453a;
    --color-error-bg: rgba(255, 69, 58, 0.15);

    /* === Sidebar 深色玻璃 === */
    --glass-dark-bg: rgba(30, 30, 32, 0.88);
    --glass-dark-border: rgba(255, 255, 255, 0.1);

    /* === 字體 === */
    --font-display: -apple-system, BlinkMacSystemFont, 'SF Pro Display', system-ui, sans-serif;
    --font-text: -apple-system, BlinkMacSystemFont, 'SF Pro Text', system-ui, sans-serif;
    --font-mono: 'SF Mono', ui-monospace, 'JetBrains Mono', monospace;

    /* === 間距 === */
    --spacing-xs: 0.25rem;   /* 4px */
    --spacing-sm: 0.5rem;    /* 8px */
    --spacing-md: 1rem;      /* 16px */
    --spacing-lg: 1.5rem;    /* 24px */
    --spacing-xl: 2rem;      /* 32px */

    /* === 圓角 === */
    --radius-sm: 12px;
    --radius-md: 16px;
    --radius-lg: 22px;
    --radius-xl: 28px;
    --radius-full: 9999px;

    /* === 動畫 === */
    --transition-fast: 180ms cubic-bezier(0.25, 0.1, 0.25, 1);
    --transition-normal: 280ms cubic-bezier(0.25, 0.1, 0.25, 1);
    --transition-bounce: 500ms cubic-bezier(0.34, 1.56, 0.64, 1);

    /* === 模糊 === */
    --blur-lg: 40px;
    --blur-xl: 60px;
}
```

---

## 常用工具類別速查表

### 間距類別

| 類別 | 值 | 常用場景 |
|------|-----|----------|
| `.mt-4` | 16px | 段落間距 |
| `.mt-6` | 24px | 區塊間距 |
| `.mt-8` | 32px | 表單按鈕與輸入框間距 |
| `.mb-4` | 16px | 標題下方間距 |
| `.mb-6` | 24px | 卡片下方間距 |
| `.gap-3` | 12px | 通知框 icon 與文字 |
| `.gap-4` | 16px | 卡片內元素間距 |
| `.space-x-4` | 16px | 按鈕群組水平間距 |
| `.space-y-6` | 24px | 表單欄位垂直間距 |

### 尺寸類別

| 類別 | 值 | 常用場景 |
|------|-----|----------|
| `.w-5` / `.h-5` | 20px | 標準 icon 尺寸 |
| `.w-6` / `.h-6` | 24px | 大 icon 尺寸 |
| `.max-w-2xl` | 672px | 表單卡片最大寬度 |

### 佈局類別

| 類別 | 說明 |
|------|------|
| `.flex` | display: flex |
| `.flex-col` | flex-direction: column |
| `.items-center` | align-items: center |
| `.justify-between` | justify-content: space-between |
| `.justify-end` | justify-content: flex-end |

### 使用範例

```html
<!-- 表單按鈕區塊（與輸入框保持 32px 間距） -->
<div class="mt-8 flex items-center justify-end space-x-4">
    <a href="/cancel" class="btn btn-secondary">Cancel</a>
    <button type="submit" class="btn btn-primary">Submit</button>
</div>

<!-- 通知框（icon 與文字 12px 間距） -->
<div class="notification">
    <div class="flex items-center gap-3">
        <svg class="w-5 h-5">...</svg>
        <span>操作成功</span>
    </div>
</div>

<!-- 表單欄位（垂直 24px 間距） -->
<div class="space-y-6">
    <div>
        <label class="form-label">Name</label>
        <input class="form-input">
    </div>
    <div>
        <label class="form-label">Email</label>
        <input class="form-input">
    </div>
</div>
```

---

## 新增頁面檢查清單

建立新頁面時，請確認：

- [ ] 使用 `.app-layout` 包裝整個頁面
- [ ] Sidebar 使用現有的 `.sidebar` 結構
- [ ] 主內容區使用 `.app-main` > `.app-header` + `.app-content`
- [ ] 卡片使用 `.card` 或 `.glass-card`（實心白色）
- [ ] 按鈕使用 `.btn` + 對應的變體類別
- [ ] 表單使用 `.form-label` + `.form-input`
- [ ] 表單按鈕區塊使用 `.mt-8` 與輸入框保持間距
- [ ] 按鈕群組使用 `.space-x-4` 保持水平間距
- [ ] 狀態標籤使用 `.badge` + 對應的狀態類別
- [ ] 響應式使用現有的 grid 類別
- [ ] 避免在內容區使用 `backdrop-filter`

---

*最後更新：2026-01-21*
