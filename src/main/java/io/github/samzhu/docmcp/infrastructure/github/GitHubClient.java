package io.github.samzhu.docmcp.infrastructure.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub API 客戶端
 * <p>
 * 用於取得 GitHub 儲存庫的文件內容、列表和 Release 資訊。
 * 支援認證和非認證模式。
 * </p>
 */
@Service
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;

    public GitHubClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${github.token:}") String githubToken
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.githubToken = githubToken;
    }

    /**
     * 取得儲存庫檔案列表
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param path  目錄路徑
     * @param ref   Git 參考（branch、tag 或 commit）
     * @return 檔案列表
     */
    public List<GitHubFile> listFiles(String owner, String repo, String path, String ref) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, ref);

        try {
            String response = executeRequest(url);
            return parseFileList(response);
        } catch (Exception e) {
            log.error("Failed to list files from GitHub: {}/{} path={}", owner, repo, path, e);
            throw new GitHubApiException("Failed to list files: " + e.getMessage(), e);
        }
    }

    /**
     * 取得檔案原始內容
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param path  檔案路徑
     * @param ref   Git 參考
     * @return 檔案內容
     */
    public String getFileContent(String owner, String repo, String path, String ref) {
        String url = String.format("%s/%s/%s/%s/%s",
                GITHUB_RAW_BASE, owner, repo, ref, path);

        try {
            return executeRequest(url);
        } catch (Exception e) {
            log.error("Failed to get file content from GitHub: {}/{} path={}", owner, repo, path, e);
            throw new GitHubApiException("Failed to get file content: " + e.getMessage(), e);
        }
    }

    /**
     * 取得 Release 列表
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @return Release 列表
     */
    public List<GitHubRelease> listReleases(String owner, String repo) {
        String url = String.format("%s/repos/%s/%s/releases",
                GITHUB_API_BASE, owner, repo);

        try {
            String response = executeRequest(url);
            return parseReleaseList(response);
        } catch (Exception e) {
            log.error("Failed to list releases from GitHub: {}/{}", owner, repo, e);
            throw new GitHubApiException("Failed to list releases: " + e.getMessage(), e);
        }
    }

    /**
     * 遞迴列出目錄中所有檔案
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     * @param path  目錄路徑
     * @param ref   Git 參考
     * @return 所有檔案列表（包含子目錄中的檔案）
     */
    public List<GitHubFile> listFilesRecursively(String owner, String repo, String path, String ref) {
        List<GitHubFile> allFiles = new ArrayList<>();
        listFilesRecursivelyInternal(owner, repo, path, ref, allFiles);
        return allFiles;
    }

    private void listFilesRecursivelyInternal(String owner, String repo, String path,
                                               String ref, List<GitHubFile> result) {
        List<GitHubFile> files = listFiles(owner, repo, path, ref);

        for (GitHubFile file : files) {
            if (file.isFile()) {
                result.add(file);
            } else if (file.isDirectory()) {
                listFilesRecursivelyInternal(owner, repo, file.path(), ref, result);
            }
        }
    }

    private String executeRequest(String url) {
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .header(HttpHeaders.USER_AGENT, "DocMCP-Server");

        if (githubToken != null && !githubToken.isBlank()) {
            request = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .header(HttpHeaders.USER_AGENT, "DocMCP-Server")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
        }

        return request.retrieve().body(String.class);
    }

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
            throw new GitHubApiException("Failed to parse file list: " + e.getMessage(), e);
        }
    }

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

    private List<GitHubRelease> parseReleaseList(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<GitHubRelease> releases = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    releases.add(parseGitHubRelease(node));
                }
            }

            return releases;
        } catch (Exception e) {
            throw new GitHubApiException("Failed to parse release list: " + e.getMessage(), e);
        }
    }

    private GitHubRelease parseGitHubRelease(JsonNode node) {
        String publishedAtStr = node.path("published_at").asText(null);
        OffsetDateTime publishedAt = publishedAtStr != null
                ? OffsetDateTime.parse(publishedAtStr)
                : null;

        return new GitHubRelease(
                node.path("id").asLong(),
                node.path("tag_name").asText(),
                node.path("name").asText(),
                node.path("body").asText(null),
                node.path("draft").asBoolean(),
                node.path("prerelease").asBoolean(),
                publishedAt,
                node.path("tarball_url").asText(null),
                node.path("zipball_url").asText(null)
        );
    }

    /**
     * GitHub API 例外
     */
    public static class GitHubApiException extends RuntimeException {
        public GitHubApiException(String message) {
            super(message);
        }

        public GitHubApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
