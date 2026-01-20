package io.github.samzhu.docmcp.infrastructure.github.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Git Tree API 策略（優先級 2）
 * <p>
 * 使用 GitHub Git Tree API 一次取得完整目錄結構。
 * </p>
 * <p>
 * 優點：
 * <ul>
 *   <li>只需 1 次 API 呼叫取得完整目錄結構</li>
 *   <li>比遞迴呼叫 Contents API 更有效率</li>
 * </ul>
 * </p>
 * <p>
 * 缺點：
 * <ul>
 *   <li>仍需要 API 呼叫（但只有 1 次）</li>
 *   <li>大型專案可能返回 "tree truncated" 錯誤</li>
 *   <li>檔案內容需要額外用 raw URL 下載</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(name = "docmcp.github.fetch.git-tree.enabled", havingValue = "true", matchIfMissing = true)
public class GitTreeFetchStrategy implements GitHubFetchStrategy {

    private static final Logger log = LoggerFactory.getLogger(GitTreeFetchStrategy.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";

    // 支援的文件副檔名
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".markdown", ".adoc", ".asciidoc", ".html", ".htm", ".txt", ".rst"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GitHubFetchProperties properties;
    private final String githubToken;

    public GitTreeFetchStrategy(
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
        return properties.getGitTree().getPriority();
    }

    @Override
    public String getName() {
        return "GitTree";
    }

    @Override
    public boolean supports(String owner, String repo, String ref) {
        // Git Tree API 支援任何 ref（branch、tag、commit）
        return true;
    }

    @Override
    public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
        // GET /repos/{owner}/{repo}/git/trees/{ref}?recursive=1
        String url = String.format("%s/repos/%s/%s/git/trees/%s?recursive=1",
                GITHUB_API_BASE, owner, repo, ref);

        log.info("嘗試使用 Git Tree API: {}", url);

        try {
            // 執行 API 請求
            String response = executeRequest(url);
            JsonNode root = objectMapper.readTree(response);

            // 檢查是否被截斷
            if (root.path("truncated").asBoolean(false)) {
                log.warn("Git Tree 被截斷（專案太大），降級到下一個策略");
                return Optional.empty();
            }

            // 解析檔案列表
            List<GitHubFile> files = parseTreeResponse(root, path);

            if (files.isEmpty()) {
                log.warn("Git Tree 中未找到任何符合條件的檔案，目標路徑: {}", path);
                return Optional.empty();
            }

            log.info("Git Tree API 成功，找到 {} 個檔案", files.size());
            return Optional.of(FetchResult.of(files, getName()));

        } catch (Exception e) {
            log.warn("Git Tree 策略失敗: {}", e.getMessage());
            return Optional.empty();
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
     * 解析 Tree API 回應
     */
    private List<GitHubFile> parseTreeResponse(JsonNode root, String targetPath) {
        List<GitHubFile> files = new ArrayList<>();
        JsonNode tree = root.path("tree");

        // 正規化目標路徑
        String normalizedPath = targetPath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (!normalizedPath.isEmpty() && !normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }

        for (JsonNode node : tree) {
            String nodePath = node.path("path").asText();
            String type = node.path("type").asText();

            // 只處理檔案（blob），跳過目錄（tree）
            if (!"blob".equals(type)) {
                continue;
            }

            // 檢查是否在目標路徑下
            if (!normalizedPath.isEmpty() && !nodePath.startsWith(normalizedPath)) {
                continue;
            }

            // 檢查是否為支援的檔案格式
            if (!isSupportedFile(nodePath)) {
                continue;
            }

            // 取得檔案名稱
            String fileName = nodePath.contains("/")
                    ? nodePath.substring(nodePath.lastIndexOf('/') + 1)
                    : nodePath;

            GitHubFile file = new GitHubFile(
                    fileName,
                    nodePath,
                    node.path("sha").asText(),
                    node.path("size").asLong(0),
                    "file",
                    null // 稍後用 raw URL 下載
            );

            files.add(file);
        }

        return files;
    }

    /**
     * 檢查是否為支援的檔案格式
     */
    private boolean isSupportedFile(String path) {
        String lowerPath = path.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }
}
