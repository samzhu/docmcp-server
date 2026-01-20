package io.github.samzhu.docmcp.infrastructure.github;

import io.github.samzhu.docmcp.infrastructure.github.strategy.FetchResult;
import io.github.samzhu.docmcp.infrastructure.github.strategy.GitHubFetchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * GitHub 內容取得器（策略協調者）
 * <p>
 * 依優先級順序嘗試各種策略取得 GitHub 內容，失敗時自動降級到下一個策略。
 * </p>
 * <p>
 * 策略優先順序（預設）：
 * <ol>
 *   <li>Archive 下載（最快、無 Rate Limit）</li>
 *   <li>Git Tree API（1 次 API 呼叫）</li>
 *   <li>Contents API（遞迴呼叫，有 Rate Limit 風險）</li>
 * </ol>
 * </p>
 */
@Service
public class GitHubContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubContentFetcher.class);
    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com";

    private final List<GitHubFetchStrategy> strategies;
    private final RestClient restClient;
    private final String githubToken;

    public GitHubContentFetcher(
            List<GitHubFetchStrategy> strategies,
            RestClient.Builder restClientBuilder,
            @Value("${github.token:}") String githubToken) {
        // 依優先級排序
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(GitHubFetchStrategy::getPriority))
                .toList();
        this.restClient = restClientBuilder.build();
        this.githubToken = githubToken;

        log.info("GitHub Content Fetcher 初始化完成，策略順序: {}",
                this.strategies.stream().map(s -> s.getName() + "(" + s.getPriority() + ")").toList());
    }

    /**
     * 取得 GitHub 內容（自動選擇最佳策略）
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param path  目錄路徑
     * @param ref   Git 參考（branch、tag 或 commit）
     * @return 取得結果
     * @throws GitHubFetchException 所有策略都失敗時拋出
     */
    public FetchResult fetch(String owner, String repo, String path, String ref) {
        log.info("開始取得 GitHub 內容: {}/{} path={} ref={}", owner, repo, path, ref);

        for (GitHubFetchStrategy strategy : strategies) {
            if (!strategy.supports(owner, repo, ref)) {
                log.debug("策略 {} 不支援此請求，跳過", strategy.getName());
                continue;
            }

            log.info("嘗試使用策略: {} (優先級 {})", strategy.getName(), strategy.getPriority());

            try {
                Optional<FetchResult> result = strategy.fetch(owner, repo, path, ref);
                if (result.isPresent()) {
                    FetchResult fetchResult = result.get();
                    log.info("策略 {} 成功，取得 {} 個檔案{}",
                            strategy.getName(),
                            fetchResult.files().size(),
                            fetchResult.contents().isEmpty() ? "" : "（含預載入內容）");
                    return fetchResult;
                } else {
                    log.warn("策略 {} 返回空結果，嘗試下一個策略", strategy.getName());
                }
            } catch (Exception e) {
                log.warn("策略 {} 失敗: {}，嘗試下一個策略", strategy.getName(), e.getMessage());
            }
        }

        throw new GitHubFetchException("所有策略都失敗，無法取得 GitHub 內容: " + owner + "/" + repo);
    }

    /**
     * 取得檔案內容
     * <p>
     * 如果 FetchResult 中有預載入內容，直接返回；
     * 否則從 raw.githubusercontent.com 下載。
     * </p>
     *
     * @param result FetchResult
     * @param owner  儲存庫擁有者
     * @param repo   儲存庫名稱
     * @param path   檔案路徑
     * @param ref    Git 參考
     * @return 檔案內容
     */
    public String getFileContent(FetchResult result, String owner, String repo, String path, String ref) {
        // 檢查是否有預載入內容
        if (result.hasContent(path)) {
            log.debug("使用預載入內容: {}", path);
            return result.getContent(path);
        }

        // 從 raw URL 下載
        return downloadRawContent(owner, repo, path, ref);
    }

    /**
     * 從 raw.githubusercontent.com 下載檔案內容
     */
    public String downloadRawContent(String owner, String repo, String path, String ref) {
        String url = String.format("%s/%s/%s/%s/%s",
                GITHUB_RAW_BASE, owner, repo, ref, path);

        log.debug("下載檔案內容: {}", url);

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "DocMCP-Server");

            // raw.githubusercontent.com 通常不需要認證，但有時有幫助
            if (githubToken != null && !githubToken.isBlank()) {
                request = restClient.get()
                        .uri(url)
                        .header(HttpHeaders.USER_AGENT, "DocMCP-Server")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
            }

            return request.retrieve().body(String.class);
        } catch (Exception e) {
            throw new GitHubFetchException("下載檔案失敗: " + path + " - " + e.getMessage(), e);
        }
    }

    /**
     * 取得可用的策略列表
     */
    public List<String> getAvailableStrategies() {
        return strategies.stream()
                .map(s -> s.getName() + " (優先級 " + s.getPriority() + ")")
                .toList();
    }

    /**
     * GitHub 取得例外
     */
    public static class GitHubFetchException extends RuntimeException {
        public GitHubFetchException(String message) {
            super(message);
        }

        public GitHubFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
