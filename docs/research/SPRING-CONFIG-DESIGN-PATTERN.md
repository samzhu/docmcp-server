# Spring Boot YAML 配置設計模式

本文件說明一套適用於多環境部署的 Spring Boot 配置設計模式，支援本地開發與 GCP 雲端部署，並相容於 GraalVM Native Image (AOT) 編譯。

## 目錄

- [設計架構概覽](#設計架構概覽)
- [雙層 Profile 設計](#雙層-profile-設計)
- [機敏值管理策略](#機敏值管理策略)
- [AOT/Native Image 相容性設計](#aotnative-image-相容性設計)
- [配置覆蓋優先順序](#配置覆蓋優先順序)
- [各環境配置內容差異](#各環境配置內容差異)
- [各檔案職責說明](#各檔案職責說明)
- [新增環境的步驟](#新增環境的步驟)
- [本地開發快速啟動](#本地開發快速啟動)
- [.gitignore 設定](#gitignore-設定)
- [設計優點總結](#設計優點總結)

---

## 設計架構概覽

```
┌──────────────────────────────────────────────────────────────────┐
│                        配置檔案分層架構                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  src/main/resources/ (打包進 Docker Image)                        │
│  ├── application.yaml          ← 基礎共用配置                      │
│  ├── application-local.yaml    ← 本地基礎設施 (如 RabbitMQ)        │
│  └── application-gcp.yaml      ← GCP 基礎設施 (如 Pub/Sub)         │
│                                                                  │
│  config/ (外部配置，不打包進 Image)                                 │
│  ├── application-aot.yaml      ← AOT 編譯專用                      │
│  ├── application-dev.yaml      ← 本地開發行為                       │
│  ├── application-lab.yaml      ← LAB 環境行為                       │
│  ├── application-sit.yaml      ← SIT 環境行為                       │
│  ├── application-uat.yaml      ← UAT 環境行為                       │
│  └── application-secrets.properties ← 本地機敏設定 (不提交 Git)      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**核心概念**：
- `src/main/resources/` 內的配置會打包進 Docker Image，包含基礎設施切換邏輯
- `config/` 內的配置為外部配置，依部署環境掛載或載入

---

## 雙層 Profile 設計

採用 **基礎設施 Profile + 環境行為 Profile** 的雙層結構：

| 類型 | Profile | 用途 |
|------|---------|------|
| **基礎設施** | `local` | 本地開發 (如 RabbitMQ、H2) |
| **基礎設施** | `gcp` | GCP 雲端 (如 Pub/Sub、Cloud SQL) |
| **環境行為** | `dev` | 開發環境 (DEBUG 日誌、100% 取樣) |
| **環境行為** | `lab` | 實驗環境 (DEBUG 日誌、100% 取樣) |
| **環境行為** | `sit` | 整合測試 (INFO 日誌、50% 取樣) |
| **環境行為** | `uat` | 驗收測試 (INFO 日誌、30% 取樣) |

### 啟動組合範例

```bash
# 本地開發 (預設)
spring.profiles.default=local,dev

# LAB 環境
spring.profiles.active=gcp,lab

# SIT 環境
spring.profiles.active=gcp,sit

# UAT 環境
spring.profiles.active=gcp,uat
```

---

## 機敏值管理策略

採用 **統一屬性名稱** 策略，本地與 GCP 使用相同的屬性名稱：

```yaml
# 屬性名稱統一為 {app}-{secret-name} 格式
# 例如：myapp-jwt-jwk-set-uri, myapp-db-password
{app}-jwt-jwk-set-uri: xxx
{app}-api-key-primary: xxx
```

### 本地開發 (`application-dev.yaml`)

```yaml
spring:
  config:
    # optional: 檔案不存在也不報錯
    import: "optional:file:./config/application-secrets.properties"

# 使用方式 - 引用 properties 中的屬性
some-config:
  api-key: ${myapp-api-key-primary}
```

### GCP 環境 (`application-gcp.yaml`)

```yaml
spring:
  config:
    import: sm@  # 啟用 Secret Manager Config Data API

# 在環境配置中使用 (如 application-lab.yaml)
# 將 Secret Manager 的值映射到統一屬性名稱
myapp-api-key-primary: ${sm@myapp-api-key-primary}
```

### 本地機敏設定範例 (`application-secrets.properties`)

```properties
# =============================================================================
# 本地開發機敏設定 (此檔案不提交到 Git)
# =============================================================================
# 屬性名稱與 GCP Secret Manager 的 secret 名稱一致
# 本地: ${myapp-xxx} 從此檔案讀取
# GCP:  ${sm@myapp-xxx} 從 Secret Manager 讀取
# =============================================================================

myapp-jwt-jwk-set-uri=https://auth.example.com/oauth2/jwks
myapp-api-key-primary=sk-your-api-key-here
```

---

## AOT/Native Image 相容性設計

### 重要原則

**不使用 `spring.autoconfigure.exclude` 排除類別，改用 `enabled: false`**

```yaml
# ❌ 錯誤做法 - AOT 編譯時無法註冊相關元件
spring:
  autoconfigure:
    exclude:
      - com.google.cloud.spring.pubsub.integration.PubSubIntegrationAutoConfiguration

# ✅ 正確做法 - AOT 可註冊，執行時禁用
spring:
  cloud:
    gcp:
      pubsub:
        enabled: false
```

### AOT 編譯專用 Profile (`application-aot.yaml`)

```yaml
# =============================================================================
# AOT 編譯專用配置
# =============================================================================
# 用途: GraalVM Native Image AOT 編譯時使用
# 重要: 不要在執行時使用此 profile
# =============================================================================

# 確保所有可能使用的 binders/元件在 AOT 編譯時都被註冊
spring:
  cloud:
    gcp:
      core:
        enabled: true
      pubsub:
        enabled: true
      secretmanager:
        enabled: false  # AOT 編譯時不需要
    stream:
      binders:
        rabbit:
          type: rabbit
        pubsub:
          type: pubsub
```

### AOT 編譯指令

```bash
# Gradle
./gradlew nativeCompile -Pspring.profiles.active=aot

# Maven
./mvnw -Pnative native:compile -Dspring.profiles.active=aot
```

---

## 配置覆蓋優先順序

Spring Boot 配置載入順序（由低到高）：

```
1. application.yaml (基礎配置)
       ↓
2. application-{profile}.yaml (Profile 配置，依 profile 順序載入)
       ↓
3. SPRING_CONFIG_ADDITIONAL_LOCATION 指定的外部配置
       ↓
4. 環境變數
```

### GCP Cloud Run 部署時的配置載入流程

```
application.yaml (Image 內)
    ↓
application-gcp.yaml (Image 內)
    ↓
/config/application-lab.yaml (Secret Manager 掛載)
    ↓
環境變數 (Cloud Run 設定)
```

---

## 各環境配置內容差異

| 配置項目 | dev | lab | sit | uat |
|----------|-----|-----|-----|-----|
| 取樣率 | 100% | 100% | 50% | 30% |
| 日誌級別 | DEBUG | DEBUG | INFO | INFO |
| Actuator 端點 | 含 env,configprops | 不含 | 不含 | 不含 |
| 機敏值來源 | 本地 properties | Secret Manager | Secret Manager | Secret Manager |

---

## 各檔案職責說明

### `application.yaml` - 基礎共用配置

```yaml
# =============================================================================
# 基礎共用配置
# =============================================================================
# 此檔案包進 Docker Image，包含所有環境共用的配置
# 環境特定配置透過 config/ 目錄下的 profile 檔案覆蓋
# =============================================================================

spring:
  application:
    name: myapp
  profiles:
    default: local,dev  # 本地開發自動使用
  threads:
    virtual:
      enabled: true     # Java 21+ Virtual Threads
  security:
    oauth2:
      resourceserver:
        jwt:
          # 預設值供 AOT 編譯使用，執行時由各環境覆蓋
          jwk-set-uri: ${myapp-jwt-jwk-set-uri:https://placeholder.example.com/.well-known/jwks.json}

server:
  tomcat:
    max-connections: 160
    accept-count: 40
    connection-timeout: 30000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

logging:
  level:
    root: INFO
```

### `application-local.yaml` - 本地基礎設施

```yaml
# =============================================================================
# 本地開發基礎設施
# =============================================================================
# Profile: local
# 用途: 本地開發環境，使用本地服務替代雲端服務
# =============================================================================

spring:
  cloud:
    # 停用 GCP 功能 (透過 property 而非 autoconfigure.exclude)
    gcp:
      core:
        enabled: false
      pubsub:
        enabled: false
      secretmanager:
        enabled: false
    stream:
      default-binder: rabbit
      bindings:
        someEvent-out-0:
          binder: rabbit
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
```

### `application-gcp.yaml` - GCP 基礎設施

```yaml
# =============================================================================
# GCP 基礎設施
# =============================================================================
# Profile: gcp
# 用途: GCP 雲端環境
#
# Secret Manager 整合:
#   - 使用 sm@ 語法從 GCP Secret Manager 讀取機敏值
#   - 語法: ${sm@secret-name} 或 ${sm@project-id/secret-name/version}
# =============================================================================

spring:
  config:
    import: sm@  # 啟用 Secret Manager Config Data API
  cloud:
    gcp:
      core:
        enabled: true
      pubsub:
        enabled: true
      secretmanager:
        enabled: true
    stream:
      bindings:
        someEvent-out-0:
          binder: pubsub
      binders:
        pubsub:
          type: pubsub

management:
  health:
    rabbit:
      enabled: false  # GCP 不使用 RabbitMQ
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}
    logging:
      export:
        otlp:
          endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}
  otlp:
    metrics:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/metrics
```

### `application-{env}.yaml` - 環境行為配置

```yaml
# =============================================================================
# {ENV} 環境配置
# =============================================================================
# 用途: xxx 環境
# 啟動: spring.profiles.active=gcp,{env}
#
# 必要的 Secrets (需先在 GCP Secret Manager 建立):
#   - {app}-config: 此配置檔內容
#   - {app}-jwt-jwk-set-uri: JWT JWKS 端點 URL
#   - {app}-api-key-primary: API Key
# =============================================================================

# 機敏屬性來源 - 從 Secret Manager 讀取，覆蓋 application.yaml 的預設值
myapp-jwt-jwk-set-uri: ${sm@myapp-jwt-jwk-set-uri}
myapp-api-key-primary: ${sm@myapp-api-key-primary}

# 業務配置
some-service:
  api:
    keys:
      - alias: "primary"
        value: ${myapp-api-key-primary}

# 可觀測性
management:
  tracing:
    sampling:
      probability: 0.5  # 依環境調整
  endpoints:
    web:
      exposure:
        # 注意: 移除 env,configprops 避免洩漏 secrets
        include: health,info,metrics,prometheus

# 日誌
logging:
  level:
    root: INFO
    com.example.myapp: INFO
```

---

## 新增環境的步驟

### 步驟 1: 建立環境行為配置檔

建立 `config/application-{env}.yaml`：

```yaml
# =============================================================================
# {ENV} 環境配置
# =============================================================================
# 用途: xxx 環境
# 啟動: spring.profiles.active=gcp,{env}
#
# 必要的 Secrets (需先在 GCP Secret Manager 建立):
#   - {app}-config: 此配置檔內容
#   - {app}-jwt-jwk-set-uri: JWT JWKS 端點 URL
#   - {app}-api-key-primary: API Key
# =============================================================================

# 機敏屬性來源 - 從 Secret Manager 讀取
myapp-jwt-jwk-set-uri: ${sm@myapp-jwt-jwk-set-uri}
myapp-api-key-primary: ${sm@myapp-api-key-primary}

# 業務配置
some-service:
  api-key: ${myapp-api-key-primary}

# 可觀測性
management:
  tracing:
    sampling:
      probability: 0.3  # 依環境調整

# 日誌
logging:
  level:
    root: INFO
    com.example.myapp: INFO
```

### 步驟 2: GCP Secret Manager 建立必要 Secrets

```bash
# 建立配置檔 Secret
gcloud secrets create {app}-config \
  --data-file=config/application-{env}.yaml

# 建立機敏值 Secrets
echo -n "https://auth.example.com/oauth2/jwks" | \
  gcloud secrets create {app}-jwt-jwk-set-uri --data-file=-

echo -n "sk-your-api-key" | \
  gcloud secrets create {app}-api-key-primary --data-file=-
```

### 步驟 3: Cloud Run 設定

```bash
gcloud run deploy {app} \
  --image=gcr.io/{project}/{app}:latest \
  --set-env-vars="SPRING_PROFILES_ACTIVE=gcp,{env}" \
  --set-env-vars="SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/" \
  --set-secrets="/config/application-{env}.yaml={app}-config:latest"
```

---

## 本地開發快速啟動

```bash
# 1. 複製機敏設定範例
cp config/application-secrets.properties.example config/application-secrets.properties

# 2. 編輯填入實際值
vim config/application-secrets.properties

# 3. 啟動應用程式 (自動使用 local,dev profile)
./gradlew bootRun

# 或指定其他 profile
./gradlew bootRun --args='--spring.profiles.active=local,sit'
```

---

## .gitignore 設定

```gitignore
# 機敏設定檔
config/application-secrets.properties

# 保留範例檔
!config/application-secrets.properties.example
```

---

## 設計優點總結

| 優點 | 說明 |
|------|------|
| **關注點分離** | 基礎設施 (local/gcp) 與環境行為 (dev/lab/sit/uat) 獨立配置 |
| **機敏值安全** | 本地 properties 不提交 Git，GCP 使用 Secret Manager |
| **AOT 相容** | 使用 `enabled: false` 而非 exclude，確保 Native Image 正常運作 |
| **統一命名** | `{app}-xxx` 屬性名稱本地與雲端一致，便於管理 |
| **易於擴展** | 新增環境只需建立對應的 profile 檔案 |
| **配置透明** | 每個檔案開頭都有完整的說明註解 |
| **預設值安全** | AOT 編譯需要的預設值使用 placeholder，執行時覆蓋 |

---

## 參考資源

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Cloud GCP Secret Manager](https://googlecloudplatform.github.io/spring-cloud-gcp/reference/html/index.html#secret-manager)
- [Spring Cloud Stream](https://docs.spring.io/spring-cloud-stream/reference/)
- [GraalVM Native Image Support](https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html)
- [Cloud Run Secret Manager Integration](https://cloud.google.com/run/docs/configuring/secrets)
