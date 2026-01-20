package io.github.samzhu.docmcp.infrastructure.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GitHub 內容取得配置
 * <p>
 * 配置各策略的啟用狀態、優先級和速率控制參數。
 * </p>
 *
 * <pre>
 * docmcp:
 *   github:
 *     fetch:
 *       archive:
 *         enabled: true
 *         priority: 1
 *       git-tree:
 *         enabled: true
 *         priority: 2
 *       contents-api:
 *         enabled: true
 *         priority: 3
 *         rate-limit:
 *           delay-ms: 100
 *           max-requests-per-sync: 500
 *           retry-count: 3
 *           retry-delay-ms: 1000
 * </pre>
 */
@ConfigurationProperties(prefix = "docmcp.github.fetch")
public class GitHubFetchProperties {

    /**
     * Archive 策略配置
     */
    private StrategyConfig archive = new StrategyConfig(true, 1);

    /**
     * Git Tree API 策略配置
     */
    private StrategyConfig gitTree = new StrategyConfig(true, 2);

    /**
     * Contents API 策略配置（含速率限制）
     */
    private ContentsApiConfig contentsApi = new ContentsApiConfig();

    /**
     * 連線超時（毫秒）
     */
    private int connectTimeoutMs = 10000;

    /**
     * 讀取超時（毫秒）
     */
    private int readTimeoutMs = 30000;

    // Getters and Setters

    public StrategyConfig getArchive() {
        return archive;
    }

    public void setArchive(StrategyConfig archive) {
        this.archive = archive;
    }

    public StrategyConfig getGitTree() {
        return gitTree;
    }

    public void setGitTree(StrategyConfig gitTree) {
        this.gitTree = gitTree;
    }

    public ContentsApiConfig getContentsApi() {
        return contentsApi;
    }

    public void setContentsApi(ContentsApiConfig contentsApi) {
        this.contentsApi = contentsApi;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * 基本策略配置
     */
    public static class StrategyConfig {
        /**
         * 是否啟用此策略
         */
        private boolean enabled = true;

        /**
         * 優先級（數字越小越優先）
         */
        private int priority;

        public StrategyConfig() {}

        public StrategyConfig(boolean enabled, int priority) {
            this.enabled = enabled;
            this.priority = priority;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    /**
     * Contents API 策略配置（含速率限制）
     */
    public static class ContentsApiConfig extends StrategyConfig {

        /**
         * 速率限制配置
         */
        private RateLimitConfig rateLimit = new RateLimitConfig();

        public ContentsApiConfig() {
            super(true, 3);
        }

        public RateLimitConfig getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimitConfig rateLimit) {
            this.rateLimit = rateLimit;
        }
    }

    /**
     * 速率限制配置
     */
    public static class RateLimitConfig {
        /**
         * 每次請求之間的延遲（毫秒）
         */
        private long delayMs = 100;

        /**
         * 單次同步的最大請求數
         */
        private int maxRequestsPerSync = 500;

        /**
         * 失敗時的重試次數
         */
        private int retryCount = 3;

        /**
         * 重試之間的延遲（毫秒）
         */
        private long retryDelayMs = 1000;

        /**
         * 遇到 Rate Limit (403/429) 時的等待時間（毫秒）
         */
        private long rateLimitWaitMs = 60000;

        public long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(long delayMs) {
            this.delayMs = delayMs;
        }

        public int getMaxRequestsPerSync() {
            return maxRequestsPerSync;
        }

        public void setMaxRequestsPerSync(int maxRequestsPerSync) {
            this.maxRequestsPerSync = maxRequestsPerSync;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public long getRetryDelayMs() {
            return retryDelayMs;
        }

        public void setRetryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
        }

        public long getRateLimitWaitMs() {
            return rateLimitWaitMs;
        }

        public void setRateLimitWaitMs(long rateLimitWaitMs) {
            this.rateLimitWaitMs = rateLimitWaitMs;
        }
    }
}
