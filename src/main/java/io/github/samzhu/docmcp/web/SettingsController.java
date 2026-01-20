package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.config.FeatureFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Settings 頁面控制器
 * <p>
 * 提供系統設定管理頁面，顯示功能開關狀態、同步排程設定及系統資訊。
 * </p>
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final FeatureFlags featureFlags;
    private final Optional<BuildProperties> buildProperties;
    private final String syncCron;

    public SettingsController(
            FeatureFlags featureFlags,
            Optional<BuildProperties> buildProperties,
            @Value("${docmcp.sync.cron:0 0 2 * * *}") String syncCron) {
        this.featureFlags = featureFlags;
        this.buildProperties = buildProperties;
        this.syncCron = syncCron;
    }

    /**
     * Settings 頁面
     */
    @GetMapping
    public String settings(Model model) {
        // 頁面資訊
        model.addAttribute("pageTitle", "Settings");
        model.addAttribute("currentPage", "settings");

        // 功能開關狀態
        model.addAttribute("webUiEnabled", featureFlags.isWebUi());
        model.addAttribute("apiKeyAuthEnabled", featureFlags.isApiKeyAuth());
        model.addAttribute("semanticSearchEnabled", featureFlags.isSemanticSearch());
        model.addAttribute("codeExamplesEnabled", featureFlags.isCodeExamples());
        model.addAttribute("migrationGuidesEnabled", featureFlags.isMigrationGuides());
        model.addAttribute("syncSchedulingEnabled", featureFlags.isSyncScheduling());
        model.addAttribute("concurrencyLimitEnabled", featureFlags.isConcurrencyLimit());

        // 同步設定
        model.addAttribute("syncCron", syncCron);

        // 系統資訊
        model.addAttribute("version", getVersion());
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("osVersion", System.getProperty("os.version"));

        return "settings/index";
    }

    /**
     * 取得應用程式版本
     */
    private String getVersion() {
        return buildProperties
                .map(BuildProperties::getVersion)
                .orElse("0.0.1-SNAPSHOT");
    }
}
