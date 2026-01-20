package io.github.samzhu.docmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 功能開關配置
 * <p>
 * 用於控制各項功能的啟用/停用狀態。
 * 配置前綴: docmcp.features
 * </p>
 */
@ConfigurationProperties(prefix = "docmcp.features")
public class FeatureFlags {

    /**
     * 是否啟用 Web UI 介面
     */
    private boolean webUi = true;

    /**
     * 是否啟用 API Key 認證
     */
    private boolean apiKeyAuth = false;

    /**
     * 是否啟用語意搜尋功能
     */
    private boolean semanticSearch = true;

    /**
     * 是否啟用程式碼範例功能
     */
    private boolean codeExamples = true;

    /**
     * 是否啟用遷移指南功能
     */
    private boolean migrationGuides = false;

    /**
     * 是否啟用同步排程功能
     */
    private boolean syncScheduling = false;

    /**
     * 是否啟用併發限制功能
     */
    private boolean concurrencyLimit = true;

    public boolean isWebUi() {
        return webUi;
    }

    public void setWebUi(boolean webUi) {
        this.webUi = webUi;
    }

    public boolean isApiKeyAuth() {
        return apiKeyAuth;
    }

    public void setApiKeyAuth(boolean apiKeyAuth) {
        this.apiKeyAuth = apiKeyAuth;
    }

    public boolean isSemanticSearch() {
        return semanticSearch;
    }

    public void setSemanticSearch(boolean semanticSearch) {
        this.semanticSearch = semanticSearch;
    }

    public boolean isCodeExamples() {
        return codeExamples;
    }

    public void setCodeExamples(boolean codeExamples) {
        this.codeExamples = codeExamples;
    }

    public boolean isMigrationGuides() {
        return migrationGuides;
    }

    public void setMigrationGuides(boolean migrationGuides) {
        this.migrationGuides = migrationGuides;
    }

    public boolean isSyncScheduling() {
        return syncScheduling;
    }

    public void setSyncScheduling(boolean syncScheduling) {
        this.syncScheduling = syncScheduling;
    }

    public boolean isConcurrencyLimit() {
        return concurrencyLimit;
    }

    public void setConcurrencyLimit(boolean concurrencyLimit) {
        this.concurrencyLimit = concurrencyLimit;
    }
}
