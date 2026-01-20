package io.github.samzhu.docmcp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KnownDocsPathsProperties 整合測試
 * <p>
 * 測試 Spring Boot 實際載入 YAML 配置的行為，
 * 驗證 relaxed binding 和版本範圍配置的正確性。
 * </p>
 */
@SpringBootTest(classes = KnownDocsPathsPropertiesIntegrationTest.TestConfig.class)
@ActiveProfiles("test-known-docs")
@DisplayName("KnownDocsPathsProperties 整合測試")
class KnownDocsPathsPropertiesIntegrationTest {

    @EnableConfigurationProperties(KnownDocsPathsProperties.class)
    static class TestConfig {
    }

    @Autowired
    private KnownDocsPathsProperties properties;

    @Test
    @DisplayName("應正確載入配置結構（除錯用）")
    @SuppressWarnings("unchecked")
    void shouldLoadConfigStructure() {
        // 驗證配置已載入
        assertThat(properties.getKnownDocsPaths())
                .as("配置應該已載入")
                .isNotEmpty();

        // 取得 spring-boot 配置
        Object springBootConfig = properties.getKnownDocsPaths().entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("spring"))
                .findFirst()
                .map(java.util.Map.Entry::getValue)
                .orElse(null);

        assertThat(springBootConfig)
                .as("應該有 spring-boot 配置，實際 keys: " + properties.getKnownDocsPaths().keySet())
                .isNotNull();

        // 檢查是否為 Map 結構（版本範圍配置）
        assertThat(springBootConfig)
                .as("spring-boot 配置應為 Map 結構，實際類型: " + springBootConfig.getClass().getName() + ", 值: " + springBootConfig)
                .isInstanceOf(java.util.Map.class);

        java.util.Map<String, Object> configMap = (java.util.Map<String, Object>) springBootConfig;

        // 檢查 versions 是否存在
        Object versionsObj = configMap.get("versions");
        assertThat(versionsObj)
                .as("應該有 versions 配置，實際 keys: " + configMap.keySet() + ", 完整 Map: " + configMap)
                .isNotNull();

        // 檢查 versions 的內容和 key
        // 注意：Spring Boot relaxed binding 會將 YAML 的 "3.*" 轉成 Integer 3
        assertThat(versionsObj)
                .as("versions 應為 Map")
                .isInstanceOf(java.util.Map.class);

        java.util.Map<?, ?> versionsMap = (java.util.Map<?, ?>) versionsObj;

        // 檢查是否有主版本 3 的配置（key 可能是 Integer 3 或 String "3"）
        boolean hasVersion3 = versionsMap.keySet().stream()
                .anyMatch(key -> "3".equals(String.valueOf(key)));

        assertThat(hasVersion3)
                .as("versions 應包含主版本 3 的配置，實際 keys: " + versionsMap.keySet() +
                        " (類型: " + versionsMap.keySet().stream().findFirst().map(k -> k.getClass().getSimpleName()).orElse("empty") + ")" +
                        ", 完整 versions: " + versionsMap)
                .isTrue();
    }

    @Test
    @DisplayName("應正確載入 Spring Boot 版本範圍配置")
    void shouldLoadSpringBootVersionConfig() {
        // 驗證配置已載入
        assertThat(properties.getKnownDocsPaths()).isNotEmpty();

        // 4.x 版本應使用預設路徑
        String path4x = properties.getDocsPath("spring-projects/spring-boot", "4.0.1");
        assertThat(path4x).isEqualTo("documentation/spring-boot-docs/src/docs/antora");

        // 3.x 版本應使用特定路徑
        String path3x = properties.getDocsPath("spring-projects/spring-boot", "3.5.9");
        assertThat(path3x).isEqualTo("spring-boot-project/spring-boot-docs/src/docs/antora");
    }

    @Test
    @DisplayName("應正確載入簡單格式配置")
    void shouldLoadSimpleConfig() {
        String reactPath = properties.getDocsPath("facebook/react", "18.0.0");
        assertThat(reactPath).isEqualTo("docs");
    }

    @Test
    @DisplayName("不帶版本號應回傳預設路徑")
    void shouldReturnDefaultWithoutVersion() {
        String defaultPath = properties.getDocsPath("spring-projects/spring-boot");
        assertThat(defaultPath).isEqualTo("documentation/spring-boot-docs/src/docs/antora");
    }

    @Test
    @DisplayName("未知函式庫應回傳 'docs'")
    void shouldReturnDocsForUnknown() {
        String unknownPath = properties.getDocsPath("unknown/repo", "1.0.0");
        assertThat(unknownPath).isEqualTo("docs");
    }
}
