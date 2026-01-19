---
name: springboot-config-organizer
description: Spring Boot 設定檔整理專家。當用戶說「整理設定檔」「整理 config」「重構 application.yaml」「設定檔太亂」「config 分層」或要求檢視/優化 Spring Boot 設定檔結構時自動使用。
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, WebSearch, WebFetch, TodoWrite, AskUserQuestion
---

# Spring Boot 設定檔整理專家

## 概述

這個 Skill 幫助用戶整理 Spring Boot 設定檔，遵循雙層 Profile 設計模式，支援本地開發與雲端部署。

```
┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
│ Phase 1 │──▶│ Phase 2 │──▶│ Phase 3 │──▶│ Phase 4 │──▶│ Phase 5 │
│  分析   │   │  規劃   │   │  確認   │   │  執行   │   │  驗證   │
└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘
     │             │             │             │             │
     ▼             ▼             ▼             ▼             ▼
  讀取現有       設計新的       與用戶確認     建立/修改      測試啟動
  設定檔結構     分層架構       整理方案       設定檔         驗證正確
```

---

## 設計原則

### 雙層 Profile 設計

| 類型 | Profile | 用途 |
|------|---------|------|
| **基礎設施** | `local` | 本地開發（Docker Compose、本地資料庫） |
| **基礎設施** | `gcp` | GCP 雲端（Cloud SQL、Secret Manager） |
| **環境行為** | `dev` | 開發環境（DEBUG 日誌、更多 Actuator） |
| **環境行為** | `sit` | 整合測試（INFO 日誌、50% 取樣） |
| **環境行為** | `uat` | 驗收測試（INFO 日誌、30% 取樣） |

### 檔案分層架構

```
src/main/resources/                    (打包進 Docker Image)
├── application.yaml                   ← 基礎共用配置
├── application-local.yaml             ← 本地基礎設施
└── application-gcp.yaml               ← GCP 基礎設施

config/                                (外部配置，不打包)
├── application-dev.yaml               ← 開發環境行為
├── application-sit.yaml               ← SIT 環境行為
├── application-secrets.properties.example  ← 範例檔（提交 Git）
└── application-secrets.properties     ← 實際機敏值（不提交 Git）
```

### 統一屬性名稱策略

```yaml
# 使用 {app}-{secret-name} 格式
# 例如：myapp-db-password, myapp-google-api-key

spring:
  datasource:
    url: ${myapp-db-url:jdbc:postgresql://localhost:5432/mydb}
    username: ${myapp-db-username:myuser}
    password: ${myapp-db-password:secret}
```

---

## Phase 1：分析現有設定檔

### 1.1 讀取所有設定檔

```bash
# 找出所有 Spring Boot 設定檔
Glob: **/application*.yaml
Glob: **/application*.yml
Glob: **/application*.properties
```

### 1.2 分析項目清單

使用 TodoWrite 記錄需要分析的項目：

```
□ 檔案結構（哪些檔案在哪裡）
□ Profile 使用方式（是否有分層）
□ 機敏值處理（密碼、API Key 在哪裡）
□ 資料庫配置位置
□ 第三方服務配置位置
□ 日誌配置
□ Actuator 配置
□ 重複配置（同一設定出現在多處）
□ 環境變數使用方式
```

### 1.3 識別問題

常見問題：

| 問題 | 說明 |
|------|------|
| **機敏值硬編碼** | 密碼、API Key 直接寫在設定檔 |
| **單一檔案過大** | 所有配置都在 application.yaml |
| **Profile 混亂** | 沒有基礎設施/環境行為分層 |
| **重複配置** | 同一設定在多處重複 |
| **缺少預設值** | 環境變數沒有預設值 |

---

## Phase 2：規劃新架構

### 2.1 決定分類

將現有配置分類到對應檔案：

```
application.yaml（基礎共用）
├── spring.application.name
├── spring.profiles.default
├── spring.threads.virtual.enabled
├── spring.datasource.*（使用統一屬性名稱）
├── spring.ai.*（AI 相關配置）
├── management.*（Actuator 基礎）
└── logging.level.root

application-local.yaml（本地基礎設施）
├── spring.docker.compose.*
└── 本地專用的連線設定

application-gcp.yaml（GCP 基礎設施）
├── spring.config.import: sm@
├── spring.cloud.gcp.*
└── GCP 專用設定

config/application-dev.yaml（開發環境行為）
├── spring.config.import（引入 secrets）
├── logging.level.*（DEBUG）
└── management.endpoints.*（更多端點）
```

### 2.2 統一屬性名稱對照表

建立對照表，追蹤機敏值：

```markdown
| 屬性名稱 | 用途 | 預設值 | 來源 |
|----------|------|--------|------|
| {app}-db-url | 資料庫 URL | jdbc:postgresql://localhost:5432/mydb | secrets.properties / Secret Manager |
| {app}-db-username | 資料庫使用者 | myuser | secrets.properties / Secret Manager |
| {app}-db-password | 資料庫密碼 | (無) | secrets.properties / Secret Manager |
| {app}-google-api-key | Google API Key | (無) | secrets.properties / Secret Manager |
```

---

## Phase 3：與用戶確認

### 3.1 報告分析結果

```markdown
## 設定檔分析報告

### 現有結構

[列出現有檔案和問題]

### 建議的新結構

[展示新的檔案分層]

### 變更摘要

| 項目 | 現有 | 建議 |
|------|------|------|
| 檔案數量 | X 個 | Y 個 |
| 機敏值處理 | 硬編碼 | 統一屬性名稱 + secrets.properties |
| Profile 分層 | 無 | 基礎設施 + 環境行為 |

### 需要確認

1. 應用程式名稱前綴：`{app}` → 建議用 `[實際名稱]`
2. 是否需要 GCP 支援？
3. 有哪些環境需要支援？（dev, sit, uat, prod）

請確認後我將開始整理。
```

### 3.2 等待用戶確認

**不要假設，等待明確指示。**

---

## Phase 4：執行整理

### 4.1 建立目錄結構

```bash
mkdir -p config
```

### 4.2 建立 application.yaml

```yaml
# =============================================================================
# {App Name} - 基礎共用配置
# =============================================================================
# 此檔案包進 Docker Image，包含所有環境共用的配置
# 環境特定配置透過 profile 檔案覆蓋
# =============================================================================

spring:
  application:
    name: {App Name}

  profiles:
    default: local,dev

  threads:
    virtual:
      enabled: true

  # ----- 資料庫配置 -----
  datasource:
    url: ${myapp-db-url:jdbc:postgresql://localhost:5432/mydb}
    username: ${myapp-db-username:myuser}
    password: ${myapp-db-password:secret}

# ----- Actuator 配置 -----
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

# ----- 日誌配置 -----
logging:
  level:
    root: INFO
```

### 4.3 建立 application-local.yaml

```yaml
# =============================================================================
# 本地開發基礎設施
# =============================================================================
# Profile: local
# 用途: 本地開發環境，使用 Docker Compose
# =============================================================================

spring:
  docker:
    compose:
      enabled: true
      lifecycle-management: start-and-stop
```

### 4.4 建立 config/application-dev.yaml

```yaml
# =============================================================================
# 開發環境行為配置
# =============================================================================
# Profile: dev
# 用途: 本地開發環境，DEBUG 日誌
# =============================================================================

spring:
  config:
    import: "optional:file:./config/application-secrets.properties"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,configprops

logging:
  level:
    com.example.myapp: DEBUG
```

### 4.5 建立 config/application-secrets.properties.example

```properties
# =============================================================================
# 本地開發機敏設定 (此檔案不提交到 Git)
# =============================================================================
# 使用方式:
#   1. 複製此檔案: cp application-secrets.properties.example application-secrets.properties
#   2. 編輯填入實際值
#   3. 啟動應用程式
# =============================================================================

myapp-db-url=jdbc:postgresql://localhost:5432/mydb
myapp-db-username=myuser
myapp-db-password=your-password-here
myapp-google-api-key=your-api-key-here
```

### 4.6 更新 .gitignore

```gitignore
### Secrets ###
config/application-secrets.properties
!config/application-secrets.properties.example
```

---

## Phase 5：驗證

### 5.1 檢查檔案結構

```bash
find . -name "application*.yaml" -o -name "application*.properties*" | grep -v build | sort
```

### 5.2 啟動測試

```bash
./gradlew bootRun
# 或
./mvnw spring-boot:run
```

### 5.3 驗證 Profile 載入

檢查啟動日誌：

```
The following profiles are active: local, dev
```

### 5.4 完成報告

```markdown
## 整理完成

### 檔案結構

```
src/main/resources/
├── application.yaml          ← 基礎共用配置
└── application-local.yaml    ← 本地基礎設施

config/
├── application-dev.yaml      ← 開發環境行為
└── application-secrets.properties.example  ← 範例檔
```

### 統一屬性名稱

| 屬性名稱 | 用途 | 預設值 |
|----------|------|--------|
| myapp-db-url | 資料庫 URL | ... |
| myapp-db-username | 資料庫使用者 | ... |
| myapp-db-password | 資料庫密碼 | (無) |

### 本地開發啟動

```bash
cp config/application-secrets.properties.example config/application-secrets.properties
# 編輯填入實際值
./gradlew bootRun
```

### 後續可擴展

- 新增 `application-gcp.yaml` 支援 GCP
- 新增 `config/application-sit.yaml` 支援 SIT 環境
```

---

## 特殊情況處理

### 已有多環境配置

如果專案已有多個環境配置，需要：
1. 識別哪些是基礎設施、哪些是環境行為
2. 重新分類到對應檔案
3. 提取共用配置到 application.yaml

### 使用 Spring Cloud Config

如果使用 Spring Cloud Config Server：
1. 保留 bootstrap.yaml
2. application.yaml 仍遵循分層原則
3. 遠端配置覆蓋本地配置

### 多模組專案

對於多模組專案：
1. 根模組放共用配置
2. 子模組放模組特定配置
3. 使用 `spring.config.import` 引入

---

## 檢查清單

```
□ application.yaml 只包含共用配置
□ 機敏值使用統一屬性名稱 ${myapp-xxx}
□ application-local.yaml 處理 Docker Compose
□ config/application-dev.yaml 引入 secrets
□ config/application-secrets.properties.example 有完整範例
□ .gitignore 排除 secrets.properties
□ 啟動測試通過
□ Profile 正確載入（local,dev）
```

---

## 參考資源

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- [Spring Boot Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html)
