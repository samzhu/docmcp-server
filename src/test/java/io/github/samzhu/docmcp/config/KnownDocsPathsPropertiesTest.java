package io.github.samzhu.docmcp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KnownDocsPathsProperties 單元測試
 * <p>
 * 測試版本範圍配置的解析和匹配邏輯
 * </p>
 */
@DisplayName("KnownDocsPathsProperties")
class KnownDocsPathsPropertiesTest {

    private KnownDocsPathsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KnownDocsPathsProperties();
    }

    @Nested
    @DisplayName("簡單格式配置")
    class SimpleFormatTests {

        @Test
        @DisplayName("應回傳簡單格式的路徑")
        void shouldReturnSimplePath() {
            // Given: 簡單格式配置（直接是字串）
            Map<String, Object> config = new HashMap<>();
            config.put("facebook/react", "docs");
            properties.setKnownDocsPaths(config);

            // When & Then
            assertThat(properties.getDocsPath("facebook/react")).isEqualTo("docs");
            assertThat(properties.getDocsPath("facebook/react", "18.0.0")).isEqualTo("docs");
            assertThat(properties.getDocsPath("facebook/react", "17.0.0")).isEqualTo("docs");
        }

        @Test
        @DisplayName("找不到配置時應回傳預設值 'docs'")
        void shouldReturnDefaultWhenNotFound() {
            // Given: 空配置
            properties.setKnownDocsPaths(new HashMap<>());

            // When & Then
            assertThat(properties.getDocsPath("unknown/repo")).isEqualTo("docs");
            assertThat(properties.getDocsPath("unknown/repo", "1.0.0")).isEqualTo("docs");
        }
    }

    @Nested
    @DisplayName("版本範圍格式配置")
    class VersionRangeFormatTests {

        @BeforeEach
        void setUpVersionConfig() {
            // 模擬 Spring Boot 配置：4.x 預設，3.x 特例
            Map<String, Object> springBootConfig = new HashMap<>();
            springBootConfig.put("default", "documentation/spring-boot-docs/src/docs/antora");

            Map<String, String> versions = new HashMap<>();
            versions.put("3.*", "spring-boot-project/spring-boot-docs/src/docs/antora");
            springBootConfig.put("versions", versions);

            Map<String, Object> config = new HashMap<>();
            config.put("spring-projects/spring-boot", springBootConfig);
            properties.setKnownDocsPaths(config);
        }

        @Test
        @DisplayName("4.x 版本應回傳預設路徑")
        void shouldReturnDefaultPathFor4x() {
            assertThat(properties.getDocsPath("spring-projects/spring-boot", "4.0.1"))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", "4.0.0"))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", "4.1.0"))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");
        }

        @Test
        @DisplayName("3.x 版本應回傳特定路徑")
        void shouldReturnVersionSpecificPathFor3x() {
            assertThat(properties.getDocsPath("spring-projects/spring-boot", "3.5.9"))
                    .isEqualTo("spring-boot-project/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", "3.4.0"))
                    .isEqualTo("spring-boot-project/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", "3.0.0"))
                    .isEqualTo("spring-boot-project/spring-boot-docs/src/docs/antora");
        }

        @Test
        @DisplayName("帶 'v' 前綴的版本號也應正確匹配")
        void shouldMatchVersionWithVPrefix() {
            assertThat(properties.getDocsPath("spring-projects/spring-boot", "v3.5.9"))
                    .isEqualTo("spring-boot-project/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", "v4.0.1"))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");
        }

        @Test
        @DisplayName("不帶版本號應回傳預設路徑")
        void shouldReturnDefaultWhenNoVersion() {
            assertThat(properties.getDocsPath("spring-projects/spring-boot"))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", null))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");

            assertThat(properties.getDocsPath("spring-projects/spring-boot", ""))
                    .isEqualTo("documentation/spring-boot-docs/src/docs/antora");
        }
    }

    @Nested
    @DisplayName("多版本範圍配置")
    class MultipleVersionRangesTests {

        @BeforeEach
        void setUpMultiVersionConfig() {
            // 模擬有多個版本範圍的配置
            Map<String, Object> libraryConfig = new HashMap<>();
            libraryConfig.put("default", "docs/v5");

            Map<String, String> versions = new HashMap<>();
            versions.put("4.*", "docs/v4");
            versions.put("3.*", "docs/v3");
            versions.put("2.1.*", "docs/v2.1");
            versions.put("2.0.0", "docs/v2.0.0-exact");
            libraryConfig.put("versions", versions);

            Map<String, Object> config = new HashMap<>();
            config.put("example/library", libraryConfig);
            properties.setKnownDocsPaths(config);
        }

        @Test
        @DisplayName("應匹配精確版本")
        void shouldMatchExactVersion() {
            assertThat(properties.getDocsPath("example/library", "2.0.0"))
                    .isEqualTo("docs/v2.0.0-exact");
        }

        @Test
        @DisplayName("應匹配次版本萬用字元")
        void shouldMatchMinorWildcard() {
            assertThat(properties.getDocsPath("example/library", "2.1.5"))
                    .isEqualTo("docs/v2.1");

            assertThat(properties.getDocsPath("example/library", "2.1.0"))
                    .isEqualTo("docs/v2.1");
        }

        @Test
        @DisplayName("應匹配主版本萬用字元")
        void shouldMatchMajorWildcard() {
            assertThat(properties.getDocsPath("example/library", "3.9.9"))
                    .isEqualTo("docs/v3");

            assertThat(properties.getDocsPath("example/library", "4.0.0"))
                    .isEqualTo("docs/v4");
        }

        @Test
        @DisplayName("未匹配的版本應回傳預設值")
        void shouldReturnDefaultForUnmatchedVersion() {
            assertThat(properties.getDocsPath("example/library", "5.0.0"))
                    .isEqualTo("docs/v5");

            assertThat(properties.getDocsPath("example/library", "6.0.0"))
                    .isEqualTo("docs/v5");
        }
    }

    @Nested
    @DisplayName("Spring Boot Relaxed Binding 相容性")
    class RelaxedBindingTests {

        @Test
        @DisplayName("應透過 normalized key 匹配（處理斜線移除）")
        void shouldMatchWithNormalizedKey() {
            // Spring Boot relaxed binding 會將 "spring-projects/spring-boot" 轉換為 "spring-projectsspring-boot"
            Map<String, Object> config = new HashMap<>();
            config.put("spring-projectsspring-boot", "docs/normalized");
            properties.setKnownDocsPaths(config);

            // 查詢時使用原始格式
            assertThat(properties.getDocsPath("spring-projects/spring-boot"))
                    .isEqualTo("docs/normalized");
        }

        @Test
        @DisplayName("大小寫不敏感匹配")
        void shouldMatchCaseInsensitive() {
            Map<String, Object> config = new HashMap<>();
            config.put("Facebook/React", "docs");
            properties.setKnownDocsPaths(config);

            assertThat(properties.getDocsPath("facebook/react")).isEqualTo("docs");
            assertThat(properties.getDocsPath("FACEBOOK/REACT")).isEqualTo("docs");
        }

        @Test
        @DisplayName("versions key 為 Integer 時應正確匹配（Spring Boot 會將 '3.*' 轉成 3）")
        void shouldMatchWhenVersionKeyIsInteger() {
            // 模擬 Spring Boot relaxed binding 的行為：YAML "3.*" 被轉成 Integer 3
            Map<String, Object> springBootConfig = new HashMap<>();
            springBootConfig.put("default", "docs/v4");

            Map<Object, String> versions = new HashMap<>();
            versions.put(3, "docs/v3");  // Integer key，模擬 Spring Boot 行為
            springBootConfig.put("versions", versions);

            Map<String, Object> config = new HashMap<>();
            config.put("example/library", springBootConfig);
            properties.setKnownDocsPaths(config);

            // 3.x 版本應匹配 Integer key 3
            assertThat(properties.getDocsPath("example/library", "3.5.9"))
                    .isEqualTo("docs/v3");

            assertThat(properties.getDocsPath("example/library", "3.0.0"))
                    .isEqualTo("docs/v3");

            // 4.x 版本應使用預設
            assertThat(properties.getDocsPath("example/library", "4.0.0"))
                    .isEqualTo("docs/v4");
        }
    }

    @Nested
    @DisplayName("hasKnownPath 方法")
    class HasKnownPathTests {

        @Test
        @DisplayName("有配置時應回傳 true")
        void shouldReturnTrueWhenConfigExists() {
            Map<String, Object> config = new HashMap<>();
            config.put("facebook/react", "docs");
            properties.setKnownDocsPaths(config);

            assertThat(properties.hasKnownPath("facebook/react")).isTrue();
        }

        @Test
        @DisplayName("無配置時應回傳 false")
        void shouldReturnFalseWhenNoConfig() {
            properties.setKnownDocsPaths(new HashMap<>());

            assertThat(properties.hasKnownPath("unknown/repo")).isFalse();
        }
    }
}
