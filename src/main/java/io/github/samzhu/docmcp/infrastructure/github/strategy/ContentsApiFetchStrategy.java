package io.github.samzhu.docmcp.infrastructure.github.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties.RateLimitConfig;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contents API 策略（優先級 3，作為最終 Fallback）
 * <p>
 * 使用 GitHub Contents API 遞迴列出檔案。
 * 這是最通用但也最慢的方式，包含完整的速率控制機制。
 * </p>
 * <p>
 * 優點：
 * <ul>
 *   <li>最通用、相容性最好</li>
 *   <li>支援任何 ref（branch、tag、commit）</li>
 * </ul>
 * </p>
 * <p>
 * 缺點：
 * <ul>
 *   <li>遞迴呼叫多次 API</li>
 *   <li>有 Rate Limit 風險（60次/小時無認證，5000次/小時有認證）</li>
 *   <li>大型專案同步時間較長</li>
 * </ul>
 * </p>
 * <p>
 * 速率控制機制：
 * <ul>
 *   <li>每次請求之間的延遲（可配置）</li>
 *   <li>單次同步的最大請求數限制</li>
 *   <li>失敗時的重試機制</li>
 *   <li>遇到 Rate Limit 時的等待機制</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "docmcp.github.fetch.contents-api.enabled", havingValue = "true", matchIfMissing = true)
public class ContentsApiFetchStrategy implements GitHubFetchStrategy {

    private static final Logger log = LoggerFactory.getLogger(ContentsApiFetchStrategy.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    // 支援的文件副檔名
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".markdown", ".adoc", ".asciidoc", ".html", ".htm", ".txt", ".rst"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GitHubFetchProperties properties;
    private final String githubToken;

    public ContentsApiFetchStrategy(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            GitHubFetchProperties properties,
            @Value("${github.token:}") String githubToken) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.githubToken = githubToken;
    }

    @Override
    public int getPriority() {
        return properties.getContentsApi().getPriority();
    }

    @Override
    public String getName() {
        return "ContentsAPI";
    }

    @Override
    public boolean supports(String owner, String repo, String ref) {
        // 作為最終 fallback，總是支援
        return true;
    }

    @Override
    public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
        log.info("使用 Contents API 遞迴列出檔案: {}/{} path={} ref={}", owner, repo, path, ref);

        RateLimitConfig rateLimitConfig = properties.getContentsApi().getRateLimit();
        AtomicInteger requestCount = new AtomicInteger(0);
        List<GitHubFile> allFiles = new ArrayList<>();

        try {
            listFilesRecursively(owner, repo, path, ref, allFiles, requestCount, rateLimitConfig);

            if (allFiles.isEmpty()) {
                log.warn("Contents API 未找到任何符合條件的檔案，目標路徑: {}", path);
                return Optional.empty();
            }

            log.info("Contents API 成功，找到 {} 個檔案，共 {} 次 API 呼叫",
                    allFiles.size(), requestCount.get());
            return Optional.of(FetchResult.of(allFiles, getName()));

        } catch (RateLimitExceededException e) {
            log.error("Contents API 達到速率限制: {}", e.getMessage());
            return Optional.empty();
        } catch (MaxRequestsExceededException e) {
            log.error("Contents API 超過最大請求數限制: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Contents API 策略失敗: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 遞迴列出目錄中所有檔案（含速率控制）
     */
    private void listFilesRecursively(String owner, String repo, String path, String ref,
                                       List<GitHubFile> result, AtomicInteger requestCount,
                                       RateLimitConfig rateLimitConfig) {
        // 檢查是否超過最大請求數
        if (requestCount.get() >= rateLimitConfig.getMaxRequestsPerSync()) {
            throw new MaxRequestsExceededException(
                    "超過單次同步最大請求數限制: " + rateLimitConfig.getMaxRequestsPerSync());
        }

        // 請求前延遲
        if (requestCount.get() > 0 && rateLimitConfig.getDelayMs() > 0) {
            try {
                Thread.sleep(rateLimitConfig.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("請求被中斷", e);
            }
        }

        // 執行請求（含重試）
        List<GitHubFile> files = executeWithRetry(owner, repo, path, ref, rateLimitConfig);
        requestCount.incrementAndGet();

        if (requestCount.get() % 10 == 0) {
            log.debug("已執行 {} 次 API 呼叫，找到 {} 個檔案...", requestCount.get(), result.size());
        }

        // 處理結果
        for (GitHubFile file : files) {
            if (file.isFile() && isSupportedFile(file.path())) {
                result.add(file);
            } else if (file.isDirectory()) {
                // 遞迴處理子目錄
                listFilesRecursively(owner, repo, file.path(), ref, result, requestCount, rateLimitConfig);
            }
        }
    }

    /**
     * 執行請求（含重試機制）
     */
    private List<GitHubFile> executeWithRetry(String owner, String repo, String path, String ref,
                                               RateLimitConfig rateLimitConfig) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= rateLimitConfig.getRetryCount()) {
            try {
                return listFiles(owner, repo, path, ref);
            } catch (HttpClientErrorException e) {
                lastException = e;

                if (e.getStatusCode() == HttpStatus.FORBIDDEN ||
                    e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    // Rate Limit 錯誤
                    log.warn("遇到 Rate Limit ({}), 等待 {}ms 後重試...",
                            e.getStatusCode(), rateLimitConfig.getRateLimitWaitMs());
                    try {
                        Thread.sleep(rateLimitConfig.getRateLimitWaitMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("等待被中斷", ie);
                    }
                } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // 路徑不存在，直接返回空列表
                    log.debug("路徑不存在: {}", path);
                    return List.of();
                } else {
                    // 其他錯誤，重試
                    log.warn("請求失敗 ({}), 重試 {}/{}...",
                            e.getStatusCode(), retryCount + 1, rateLimitConfig.getRetryCount());
                    try {
                        Thread.sleep(rateLimitConfig.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("等待被中斷", ie);
                    }
                }
                retryCount++;
            } catch (Exception e) {
                lastException = e;
                log.warn("請求失敗, 重試 {}/{}...: {}",
                        retryCount + 1, rateLimitConfig.getRetryCount(), e.getMessage());
                try {
                    Thread.sleep(rateLimitConfig.getRetryDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待被中斷", ie);
                }
                retryCount++;
            }
        }

        throw new RuntimeException("請求失敗，已重試 " + rateLimitConfig.getRetryCount() + " 次", lastException);
    }

    /**
     * 列出目錄檔案
     */
    private List<GitHubFile> listFiles(String owner, String repo, String path, String ref) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, ref);

        try {
            String response = executeRequest(url);
            return parseFileList(response);
        } catch (Exception e) {
            throw new RuntimeException("列出檔案失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 執行 API 請求
     */
    private String executeRequest(String url) {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .header(HttpHeaders.USER_AGENT, "DocMCP-Server");

        // 如果有 token，加上認證標頭
        if (githubToken != null && !githubToken.isBlank()) {
            request = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .header(HttpHeaders.USER_AGENT, "DocMCP-Server")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
        }

        return request.retrieve().body(String.class);
    }

    /**
     * 解析檔案列表
     */
    private List<GitHubFile> parseFileList(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<GitHubFile> files = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    files.add(parseGitHubFile(node));
                }
            }

            return files;
        } catch (Exception e) {
            throw new RuntimeException("解析檔案列表失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 解析單一檔案
     */
    private GitHubFile parseGitHubFile(JsonNode node) {
        return new GitHubFile(
                node.path("name").asText(),
                node.path("path").asText(),
                node.path("sha").asText(),
                node.path("size").asLong(),
                node.path("type").asText(),
                node.path("download_url").asText(null)
        );
    }

    /**
     * 檢查是否為支援的檔案格式
     */
    private boolean isSupportedFile(String path) {
        String lowerPath = path.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    /**
     * Rate Limit 超限例外
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * 最大請求數超限例外
     */
    public static class MaxRequestsExceededException extends RuntimeException {
        public MaxRequestsExceededException(String message) {
            super(message);
        }
    }
}
