package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.config.FeatureFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SettingsController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
@DisplayName("SettingsController")
class SettingsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private FeatureFlags featureFlags;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("should show settings page with all feature flags")
    void shouldShowSettingsPageWithAllFeatureFlags() throws Exception {
        // Arrange
        when(featureFlags.isWebUi()).thenReturn(true);
        when(featureFlags.isApiKeyAuth()).thenReturn(false);
        when(featureFlags.isSemanticSearch()).thenReturn(true);
        when(featureFlags.isCodeExamples()).thenReturn(true);
        when(featureFlags.isMigrationGuides()).thenReturn(false);
        when(featureFlags.isSyncScheduling()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/index"))
                .andExpect(model().attribute("pageTitle", "Settings"))
                .andExpect(model().attribute("currentPage", "settings"))
                .andExpect(model().attribute("webUiEnabled", true))
                .andExpect(model().attribute("apiKeyAuthEnabled", false))
                .andExpect(model().attribute("semanticSearchEnabled", true))
                .andExpect(model().attribute("codeExamplesEnabled", true))
                .andExpect(model().attribute("migrationGuidesEnabled", false))
                .andExpect(model().attribute("syncSchedulingEnabled", false));
    }

    @Test
    @WithMockUser
    @DisplayName("should display sync cron schedule")
    void shouldDisplaySyncCronSchedule() throws Exception {
        // Arrange
        when(featureFlags.isWebUi()).thenReturn(true);
        when(featureFlags.isApiKeyAuth()).thenReturn(false);
        when(featureFlags.isSemanticSearch()).thenReturn(true);
        when(featureFlags.isCodeExamples()).thenReturn(true);
        when(featureFlags.isMigrationGuides()).thenReturn(false);
        when(featureFlags.isSyncScheduling()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("syncCron"));
    }

    @Test
    @WithMockUser
    @DisplayName("should display system information")
    void shouldDisplaySystemInformation() throws Exception {
        // Arrange
        when(featureFlags.isWebUi()).thenReturn(true);
        when(featureFlags.isApiKeyAuth()).thenReturn(false);
        when(featureFlags.isSemanticSearch()).thenReturn(true);
        when(featureFlags.isCodeExamples()).thenReturn(true);
        when(featureFlags.isMigrationGuides()).thenReturn(false);
        when(featureFlags.isSyncScheduling()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("version"))
                .andExpect(model().attributeExists("javaVersion"))
                .andExpect(model().attributeExists("osName"))
                .andExpect(model().attributeExists("osVersion"));
    }

    @Test
    @WithMockUser
    @DisplayName("should show enabled feature flags correctly")
    void shouldShowEnabledFeatureFlagsCorrectly() throws Exception {
        // Arrange - all features enabled
        when(featureFlags.isWebUi()).thenReturn(true);
        when(featureFlags.isApiKeyAuth()).thenReturn(true);
        when(featureFlags.isSemanticSearch()).thenReturn(true);
        when(featureFlags.isCodeExamples()).thenReturn(true);
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        when(featureFlags.isSyncScheduling()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("apiKeyAuthEnabled", true))
                .andExpect(model().attribute("migrationGuidesEnabled", true))
                .andExpect(model().attribute("syncSchedulingEnabled", true));
    }
}
