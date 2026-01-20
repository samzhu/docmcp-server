# OpenFeature Feature Flags 功能計劃書

> **版本**: 1.0.0
> **建立日期**: 2026-01-20
> **狀態**: Draft
> **參考規範**: [OpenFeature Specification](https://openfeature.dev/specification/)

---

## 1. 背景與目標

### 1.1 背景

目前 DocMCP Server 使用自定義的 `FeatureFlags` 類別透過 Spring Boot `@ConfigurationProperties` 實現功能開關。此實作雖然簡單有效，但存在以下限制：

- **缺乏標準化**：自定義 API 無法與業界工具整合
- **無動態更新能力**：變更 Feature Flag 需要重新部署
- **缺少 Evaluation Context**：無法根據用戶、環境等上下文動態決策
- **無 Audit Trail**：缺乏 Flag 評估的追蹤與稽核機制

### 1.2 目標

導入 [OpenFeature](https://openfeature.dev/) 標準，實現：

1. **標準化 API**：符合 OpenFeature 規範，支援多種 Provider 切換
2. **動態評估**：支援基於 Evaluation Context 的動態功能開關
3. **可觀測性**：透過 Hooks 實現評估追蹤與日誌
4. **向後相容**：保留現有 YAML 配置方式作為預設 Provider
5. **漸進式遷移**：現有程式碼可無縫過渡

---

## 2. OpenFeature 規範概覽

### 2.1 核心概念

```
┌─────────────────────────────────────────────────────────────────┐
│                      OpenFeature Architecture                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐     ┌──────────┐     ┌────────────────────────┐   │
│  │   API    │────▶│  Client  │────▶│       Provider         │   │
│  │(Singleton)│     │ (Domain) │     │ (Flag Evaluation)      │   │
│  └──────────┘     └──────────┘     └────────────────────────┘   │
│       │                │                      │                  │
│       │                │                      │                  │
│       ▼                ▼                      ▼                  │
│  ┌──────────┐     ┌──────────┐     ┌────────────────────────┐   │
│  │  Hooks   │     │ Context  │     │   Resolution Details   │   │
│  │(Lifecycle)│     │(Targeting)│     │   (value, reason)      │   │
│  └──────────┘     └──────────┘     └────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Flag Evaluation API

根據 OpenFeature 規範，必須支援四種類型的 Flag 評估：

| 方法 | 返回類型 | 用途 |
|------|----------|------|
| `getBooleanValue()` | `boolean` | 開關類功能 |
| `getStringValue()` | `String` | 變體選擇、版本號 |
| `getNumberValue()` | `Number` | 閾值、限制數值 |
| `getObjectValue<T>()` | `T` | 複雜配置物件 |

每種類型都有對應的 `*Details()` 方法，返回完整的評估詳情：

```java
// 簡單評估
boolean enabled = client.getBooleanValue("feature-x", false);

// 詳細評估
FlagEvaluationDetails<Boolean> details = client.getBooleanDetails(
    "feature-x",
    false,
    evaluationContext
);
// details.getValue()      - 評估結果
// details.getVariant()    - 變體標識
// details.getReason()     - 評估原因 (STATIC, TARGETING_MATCH, etc.)
// details.getErrorCode()  - 錯誤碼 (若有)
```

### 2.3 Provider Interface

Provider 是實際執行 Flag 評估的元件，必須實作以下介面：

```java
public interface FeatureProvider {
    // 中繼資料
    Metadata getMetadata();

    // 生命週期
    void initialize(EvaluationContext context) throws Exception;
    void shutdown();

    // Flag 評估方法
    ProviderEvaluation<Boolean> getBooleanEvaluation(
        String key, Boolean defaultValue, EvaluationContext ctx);
    ProviderEvaluation<String> getStringEvaluation(
        String key, String defaultValue, EvaluationContext ctx);
    ProviderEvaluation<Double> getDoubleEvaluation(
        String key, Double defaultValue, EvaluationContext ctx);
    ProviderEvaluation<Value> getObjectEvaluation(
        String key, Value defaultValue, EvaluationContext ctx);
}
```

### 2.4 Evaluation Context

Evaluation Context 提供 Flag 評估的上下文資訊：

```java
EvaluationContext context = new ImmutableContext()
    .add("targetingKey", "user-123")        // 目標識別鍵
    .add("environment", "production")        // 環境
    .add("apiKeyId", "ak-456")              // API Key ID
    .add("userTier", "premium");            // 用戶層級
```

**Context 合併優先順序** (由低到高)：
1. API/Global Context
2. Transaction Context
3. Client Context
4. Invocation Context
5. Before Hook Context

### 2.5 Hooks

Hooks 在 Flag 評估的生命週期中注入行為：

```
┌─────────────────────────────────────────────────────────┐
│                   Hook Lifecycle                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│   before() ──▶ [Flag Evaluation] ──▶ after()            │
│      │                 │                │                │
│      │                 │                │                │
│      └────────────────▶│◀───────────────┘                │
│                        │                                 │
│                        ▼                                 │
│                    error()  (若發生錯誤)                  │
│                        │                                 │
│                        ▼                                 │
│                   finally() (總是執行)                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 2.6 Events

Provider 事件用於通知狀態變更：

| 事件 | 說明 |
|------|------|
| `PROVIDER_READY` | Provider 初始化完成 |
| `PROVIDER_ERROR` | 發生可恢復錯誤 |
| `PROVIDER_FATAL` | 發生不可恢復錯誤 |
| `PROVIDER_STALE` | 快取資料可能過時 |
| `PROVIDER_CONFIGURATION_CHANGED` | Flag 配置已變更 |

---

## 3. 現有系統分析

### 3.1 現有 Feature Flags

```yaml
# 目前的 YAML 配置方式
docmcp:
  features:
    web-ui: true
    api-key-auth: false
    semantic-search: true
    code-examples: true
    migration-guides: false
    sync-scheduling: false
    concurrency-limit: true
```

### 3.2 現有使用方式

```java
@Component
public class SomeService {
    private final FeatureFlags featureFlags;

    public void doSomething() {
        if (featureFlags.isSemanticSearch()) {
            // 執行語意搜尋
        }
    }
}
```

### 3.3 需重構的使用點

| 元件 | 使用的 Flag | 檔案位置 |
|------|------------|----------|
| `GetMigrationGuideTool` | `migrationGuides` | `mcp/tool/retrieve/` |
| `ConcurrencyLimitInterceptor` | `concurrencyLimit` | `security/` |
| `SecurityConfig` | `apiKeyAuth` | `config/` |
| `SettingsController` | 所有 Flags | `web/` |
| `SyncScheduler` | `syncScheduling` | `service/` |

---

## 4. 架構設計

### 4.1 目標架構

```
┌─────────────────────────────────────────────────────────────────┐
│                    DocMCP OpenFeature Architecture               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   OpenFeature API                        │    │
│  │                    (Singleton)                           │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                  OpenFeature Client                      │    │
│  │              (Spring Bean - Injectable)                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                     │
│           ┌────────────────┼────────────────┐                    │
│           ▼                ▼                ▼                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐      │
│  │  Logging    │  │  Metrics    │  │   Transaction       │      │
│  │   Hook      │  │   Hook      │  │   Context Hook      │      │
│  └─────────────┘  └─────────────┘  └─────────────────────┘      │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                     Provider                             │    │
│  │  ┌───────────────┐  ┌───────────────┐  ┌─────────────┐  │    │
│  │  │    YAML       │  │    flagd      │  │   Custom    │  │    │
│  │  │  Provider     │  │   Provider    │  │  Provider   │  │    │
│  │  │  (Default)    │  │  (Optional)   │  │ (Extensible)│  │    │
│  │  └───────────────┘  └───────────────┘  └─────────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 套件結構

```
io.github.samzhu.docmcp.feature/
├── config/
│   └── OpenFeatureConfig.java          # Spring 配置類
├── provider/
│   ├── YamlFeatureProvider.java        # YAML 檔案 Provider
│   └── InMemoryFeatureProvider.java    # 測試用 Provider
├── hook/
│   ├── LoggingHook.java                # 日誌 Hook
│   ├── MetricsHook.java                # 指標 Hook
│   └── TransactionContextHook.java     # 請求上下文 Hook
├── context/
│   └── DocMcpContextProvider.java      # 上下文提供者
└── adapter/
    └── FeatureFlagsAdapter.java        # 向後相容適配器
```

### 4.3 Flag 定義格式

採用 JSON 格式定義 Flags，支援更豐富的配置：

```json
{
  "flags": {
    "web-ui": {
      "state": "ENABLED",
      "variants": {
        "on": true,
        "off": false
      },
      "defaultVariant": "on",
      "targeting": {}
    },
    "api-key-auth": {
      "state": "ENABLED",
      "variants": {
        "on": true,
        "off": false
      },
      "defaultVariant": "off",
      "targeting": {
        "if": [
          { "in": ["production", { "var": "environment" }] },
          "on",
          "off"
        ]
      }
    },
    "concurrent-request-limit": {
      "state": "ENABLED",
      "variants": {
        "low": 5,
        "medium": 10,
        "high": 20,
        "unlimited": -1
      },
      "defaultVariant": "medium",
      "targeting": {
        "if": [
          { "==": [{ "var": "userTier" }, "premium"] },
          "high",
          { "if": [
            { "==": [{ "var": "environment" }, "test"] },
            "unlimited",
            "medium"
          ]}
        ]
      }
    }
  }
}
```

---

## 5. 實作階段

### 5.1 Phase 1: 基礎建設 (Foundation)

**目標**: 導入 OpenFeature SDK 並建立基礎架構

**任務清單**:

- [ ] 新增 OpenFeature SDK 依賴
  ```gradle
  implementation 'dev.openfeature:sdk:1.20.0'
  ```

- [ ] 建立 `OpenFeatureConfig` 配置類
  - 配置 OpenFeatureAPI singleton
  - 註冊預設 Provider
  - 建立 Client Bean

- [ ] 實作 `YamlFeatureProvider`
  - 讀取現有 `application.yaml` 配置
  - 實作四種類型的評估方法
  - 支援熱重載 (透過 `@RefreshScope`)

- [ ] 建立 `FeatureFlagsAdapter`
  - 提供與現有 `FeatureFlags` 相同的 API
  - 內部委派給 OpenFeature Client
  - 確保向後相容

**交付物**:
- `OpenFeatureConfig.java`
- `YamlFeatureProvider.java`
- `FeatureFlagsAdapter.java`
- 單元測試

### 5.2 Phase 2: Hooks 與可觀測性 (Observability)

**目標**: 實作 Hooks 機制提供可觀測性

**任務清單**:

- [ ] 實作 `LoggingHook`
  - 在 `before` 階段記錄評估請求
  - 在 `after` 階段記錄評估結果
  - 在 `error` 階段記錄錯誤詳情
  - 使用 SLF4J 結構化日誌

- [ ] 實作 `MetricsHook`
  - 收集評估次數 (by flag key)
  - 收集評估延遲 (histogram)
  - 收集錯誤率 (by error code)
  - 整合 Micrometer

- [ ] 實作 `TransactionContextHook`
  - 從 Spring Security Context 提取用戶資訊
  - 從 HTTP Request 提取環境資訊
  - 自動填充 Evaluation Context

**交付物**:
- `LoggingHook.java`
- `MetricsHook.java`
- `TransactionContextHook.java`
- Grafana Dashboard 範本

### 5.3 Phase 3: 進階評估 (Advanced Evaluation)

**目標**: 支援基於上下文的動態評估

**任務清單**:

- [ ] 實作 Targeting 規則引擎
  - 支援 JSONLogic 格式的規則
  - 支援百分比分流 (Percentage Rollout)
  - 支援用戶分群 (User Segmentation)

- [ ] 實作 `DocMcpContextProvider`
  - 從 API Key 提取 tenant 資訊
  - 從 Request 提取 IP、User-Agent
  - 支援自定義屬性

- [ ] 擴展 Provider 支援多種類型
  - String variants (A/B 測試)
  - Number variants (限流配置)
  - Object variants (複雜配置)

**交付物**:
- `TargetingRuleEngine.java`
- `DocMcpContextProvider.java`
- 進階評估單元測試

### 5.4 Phase 4: 外部 Provider 支援 (External Providers)

**目標**: 支援外部 Feature Flag 服務

**任務清單**:

- [ ] 整合 flagd Provider (可選)
  ```gradle
  implementation 'dev.openfeature.contrib.providers:flagd:0.11.10'
  ```

- [ ] 實作 Provider 切換機制
  - 透過配置選擇 Provider
  - 支援 Provider 降級 (Fallback)
  - 支援 Provider 熱切換

- [ ] 實作 Events 處理
  - 監聽 `PROVIDER_CONFIGURATION_CHANGED`
  - 觸發快取清除
  - 通知相關元件

**交付物**:
- `FlagdProviderConfig.java`
- `ProviderFallbackHandler.java`
- 整合測試

### 5.5 Phase 5: Web UI 整合 (UI Integration)

**目標**: 在 Web UI 中提供 Feature Flag 管理

**任務清單**:

- [ ] 擴展 Settings 頁面
  - 顯示所有 Flag 及其狀態
  - 顯示 Flag 評估統計
  - 顯示 Provider 狀態

- [ ] 實作 Flag 管理 API
  - `GET /api/features` - 列出所有 Flags
  - `GET /api/features/{key}` - 取得 Flag 詳情
  - `POST /api/features/{key}/evaluate` - 測試評估

- [ ] (可選) 實作動態修改
  - 僅限開發/測試環境
  - 需要管理員權限
  - 記錄變更日誌

**交付物**:
- 擴展的 Settings 頁面
- Feature Flag 管理 API
- API 文件

---

## 6. 測試策略

### 6.1 單元測試

```java
@ExtendWith(MockitoExtension.class)
class YamlFeatureProviderTest {

    @Test
    void shouldReturnDefaultValueWhenFlagNotFound() {
        // Given
        YamlFeatureProvider provider = new YamlFeatureProvider(emptyConfig());

        // When
        ProviderEvaluation<Boolean> result = provider.getBooleanEvaluation(
            "unknown-flag", false, new ImmutableContext()
        );

        // Then
        assertThat(result.getValue()).isFalse();
        assertThat(result.getReason()).isEqualTo(Reason.DEFAULT.toString());
    }

    @Test
    void shouldEvaluateTargetingRule() {
        // Given
        YamlFeatureProvider provider = new YamlFeatureProvider(configWithTargeting());
        EvaluationContext ctx = new ImmutableContext()
            .add("environment", "production");

        // When
        ProviderEvaluation<Boolean> result = provider.getBooleanEvaluation(
            "api-key-auth", false, ctx
        );

        // Then
        assertThat(result.getValue()).isTrue();
        assertThat(result.getReason()).isEqualTo(Reason.TARGETING_MATCH.toString());
    }
}
```

### 6.2 整合測試

```java
@SpringBootTest
@ActiveProfiles("test")
class OpenFeatureIntegrationTest {

    @Autowired
    private Client openFeatureClient;

    @Test
    void shouldEvaluateFlagsWithSpringContext() {
        // Given
        EvaluationContext ctx = new ImmutableContext()
            .add("targetingKey", "test-user");

        // When
        boolean webUiEnabled = openFeatureClient.getBooleanValue("web-ui", false, ctx);

        // Then
        assertThat(webUiEnabled).isTrue();
    }

    @Test
    void shouldWorkWithSecurityContext() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(
            new ApiKeyAuthentication("test-api-key")
        );

        // When
        FlagEvaluationDetails<Integer> details = openFeatureClient.getIntegerDetails(
            "concurrent-request-limit", 10
        );

        // Then
        assertThat(details.getValue()).isEqualTo(10);
        assertThat(details.getReason()).isEqualTo("STATIC");
    }
}
```

### 6.3 Hook 測試

```java
@Test
void loggingHookShouldLogEvaluationDetails() {
    // Given
    LoggingHook hook = new LoggingHook();
    HookContext<Boolean> context = createHookContext("test-flag", Boolean.class);

    try (LogCaptor logCaptor = LogCaptor.forClass(LoggingHook.class)) {
        // When
        hook.before(context, Map.of());
        hook.after(context, createDetails(true), Map.of());

        // Then
        assertThat(logCaptor.getInfoLogs())
            .anyMatch(log -> log.contains("test-flag"))
            .anyMatch(log -> log.contains("value=true"));
    }
}
```

---

## 7. 遷移計劃

### 7.1 遷移策略

採用 **Adapter Pattern** 實現無縫遷移：

```java
/**
 * 向後相容適配器
 * <p>
 * 提供與原 FeatureFlags 相同的 API，內部委派給 OpenFeature Client。
 * 現有程式碼無需修改即可繼續運作。
 * </p>
 */
@Component
public class FeatureFlagsAdapter {

    private final Client openFeatureClient;

    public FeatureFlagsAdapter(Client openFeatureClient) {
        this.openFeatureClient = openFeatureClient;
    }

    public boolean isWebUi() {
        return openFeatureClient.getBooleanValue("web-ui", true);
    }

    public boolean isApiKeyAuth() {
        return openFeatureClient.getBooleanValue("api-key-auth", false);
    }

    public boolean isSemanticSearch() {
        return openFeatureClient.getBooleanValue("semantic-search", true);
    }

    // ... 其他方法
}
```

### 7.2 遷移步驟

| 步驟 | 說明 | 風險 |
|------|------|------|
| 1 | 新增 OpenFeature 依賴 | 低 |
| 2 | 實作 YamlFeatureProvider | 低 |
| 3 | 建立 FeatureFlagsAdapter | 低 |
| 4 | 將 FeatureFlags 替換為 Adapter | 中 |
| 5 | 逐步遷移至直接使用 Client | 低 |
| 6 | 移除舊的 FeatureFlags 類別 | 中 |

### 7.3 回滾計劃

若遷移過程中發現問題：

1. **Phase 1-3**：可直接回滾，因為使用 Adapter 保持向後相容
2. **Phase 4-5**：透過配置切換回 YAML Provider
3. **完全回滾**：恢復原 FeatureFlags 類別

---

## 8. 風險評估

### 8.1 技術風險

| 風險 | 影響 | 可能性 | 緩解措施 |
|------|------|--------|----------|
| OpenFeature SDK 相容性問題 | 高 | 低 | 鎖定穩定版本、充分測試 |
| 效能影響 | 中 | 低 | 實作快取、效能測試 |
| 配置遷移錯誤 | 中 | 中 | 自動化測試、逐步遷移 |

### 8.2 業務風險

| 風險 | 影響 | 可能性 | 緩解措施 |
|------|------|--------|----------|
| 功能開關誤判 | 高 | 低 | 保守的預設值、監控告警 |
| 遷移期間服務中斷 | 高 | 低 | 漸進式遷移、回滾機制 |

---

## 9. 時程規劃

| Phase | 名稱 | 依賴 | 優先級 |
|-------|------|------|--------|
| 1 | 基礎建設 | 無 | P0 |
| 2 | Hooks 與可觀測性 | Phase 1 | P1 |
| 3 | 進階評估 | Phase 1 | P1 |
| 4 | 外部 Provider 支援 | Phase 1-3 | P2 |
| 5 | Web UI 整合 | Phase 1-3 | P2 |

---

## 10. 參考資料

- [OpenFeature Specification](https://openfeature.dev/specification/)
- [OpenFeature Java SDK](https://openfeature.dev/docs/reference/sdks/server/java/)
- [OpenFeature Spring Boot Tutorial](https://openfeature.dev/docs/tutorials/getting-started/java/spring-boot/)
- [OpenFeature Java SDK GitHub](https://github.com/open-feature/java-sdk)
- [flagd Provider](https://github.com/open-feature/java-sdk-contrib/tree/main/providers/flagd)

---

## 附錄 A: API 範例

### A.1 Flag 評估

```java
// 簡單 Boolean 評估
boolean enabled = client.getBooleanValue("feature-x", false);

// 帶上下文評估
EvaluationContext ctx = new ImmutableContext()
    .add("targetingKey", apiKeyId)
    .add("environment", "production");

FlagEvaluationDetails<Boolean> details = client.getBooleanDetails(
    "feature-x", false, ctx
);

// 數值類型
int limit = client.getIntegerValue("rate-limit", 100);

// 字串類型 (A/B 測試)
String variant = client.getStringValue("button-color", "blue");

// 物件類型
Value config = client.getObjectValue("complex-config", defaultConfig);
```

### A.2 Hook 實作

```java
public class LoggingHook implements Hook<Object> {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    @Override
    public Optional<EvaluationContext> before(
            HookContext<Object> ctx,
            Map<String, Object> hints) {
        log.debug("Evaluating flag: {} for targeting key: {}",
            ctx.getFlagKey(),
            ctx.getCtx().getTargetingKey());
        return Optional.empty();
    }

    @Override
    public void after(
            HookContext<Object> ctx,
            FlagEvaluationDetails<Object> details,
            Map<String, Object> hints) {
        log.info("Flag {} evaluated to {} (reason: {})",
            ctx.getFlagKey(),
            details.getValue(),
            details.getReason());
    }

    @Override
    public void error(
            HookContext<Object> ctx,
            Exception error,
            Map<String, Object> hints) {
        log.error("Error evaluating flag {}: {}",
            ctx.getFlagKey(),
            error.getMessage());
    }
}
```

---

## 附錄 B: 配置範例

### B.1 application.yaml

```yaml
docmcp:
  feature:
    provider: yaml  # 可選: yaml, flagd, custom
    yaml:
      path: classpath:feature-flags.json
    flagd:
      host: localhost
      port: 8013
      tls: false
    hooks:
      logging:
        enabled: true
        level: DEBUG
      metrics:
        enabled: true
        prefix: docmcp.feature
```

### B.2 feature-flags.json

```json
{
  "$schema": "https://flagd.dev/schema/v0/flags.json",
  "flags": {
    "web-ui": {
      "state": "ENABLED",
      "variants": {
        "on": true,
        "off": false
      },
      "defaultVariant": "on"
    },
    "semantic-search": {
      "state": "ENABLED",
      "variants": {
        "on": true,
        "off": false
      },
      "defaultVariant": "on",
      "targeting": {
        "if": [
          { "==": [{ "var": "environment" }, "test"] },
          "off",
          "on"
        ]
      }
    }
  }
}
```

---

**文件結束**
