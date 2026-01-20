package io.github.samzhu.docmcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FeatureFlags 功能開關配置測試
 */
@SpringBootTest(classes = FeatureFlagsTest.TestConfig.class)
@TestPropertySource(properties = {
        "docmcp.features.web-ui=true",
        "docmcp.features.api-key-auth=false",
        "docmcp.features.semantic-search=true",
        "docmcp.features.code-examples=true",
        "docmcp.features.migration-guides=false",
        "docmcp.features.sync-scheduling=false"
})
class FeatureFlagsTest {

    @Autowired
    private FeatureFlags featureFlags;

    @Test
    void shouldLoadWebUiFeatureFlag() {
        assertThat(featureFlags.isWebUi()).isTrue();
    }

    @Test
    void shouldLoadApiKeyAuthFeatureFlag() {
        assertThat(featureFlags.isApiKeyAuth()).isFalse();
    }

    @Test
    void shouldLoadSemanticSearchFeatureFlag() {
        assertThat(featureFlags.isSemanticSearch()).isTrue();
    }

    @Test
    void shouldLoadCodeExamplesFeatureFlag() {
        assertThat(featureFlags.isCodeExamples()).isTrue();
    }

    @Test
    void shouldLoadMigrationGuidesFeatureFlag() {
        assertThat(featureFlags.isMigrationGuides()).isFalse();
    }

    @Test
    void shouldLoadSyncSchedulingFeatureFlag() {
        assertThat(featureFlags.isSyncScheduling()).isFalse();
    }

    @EnableConfigurationProperties(FeatureFlags.class)
    static class TestConfig {
    }
}
