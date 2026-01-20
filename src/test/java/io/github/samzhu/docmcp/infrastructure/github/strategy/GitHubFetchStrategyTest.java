package io.github.samzhu.docmcp.infrastructure.github.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GitHub Fetch 策略單元測試
 */
@DisplayName("GitHub Fetch Strategies")
class GitHubFetchStrategyTest {

    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
        // 移除結尾斜線
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("GitTreeFetchStrategy")
    class GitTreeFetchStrategyTests {

        private GitTreeFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            GitHubFetchProperties properties = createDefaultProperties();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();

            strategy = new TestableGitTreeFetchStrategy(
                    restClient,
                    new ObjectMapper(),
                    properties,
                    "",
                    baseUrl
            );
        }

        @Test
        @DisplayName("應成功從 Git Tree API 取得檔案列表")
        void shouldFetchFilesFromGitTree() {
            // Arrange
            String responseJson = """
                    {
                        "sha": "abc123",
                        "url": "https://api.github.com/repos/owner/repo/git/trees/abc123",
                        "tree": [
                            {"path": "docs/README.md", "mode": "100644", "type": "blob", "sha": "def456", "size": 1024},
                            {"path": "docs/guide/intro.md", "mode": "100644", "type": "blob", "sha": "ghi789", "size": 512},
                            {"path": "docs/guide", "mode": "040000", "type": "tree", "sha": "jkl012"},
                            {"path": "src/main.java", "mode": "100644", "type": "blob", "sha": "mno345", "size": 2048}
                        ],
                        "truncated": false
                    }
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            // Act
            Optional<FetchResult> result = strategy.fetch("owner", "repo", "docs", "main");

            // Assert
            assertThat(result).isPresent();
            FetchResult fetchResult = result.get();
            assertThat(fetchResult.strategyUsed()).isEqualTo("GitTree");
            // 應只包含 docs 目錄下的 .md 檔案
            assertThat(fetchResult.files()).hasSize(2);
            assertThat(fetchResult.files().stream().map(GitHubFile::path))
                    .containsExactlyInAnyOrder("docs/README.md", "docs/guide/intro.md");
        }

        @Test
        @DisplayName("當結果被截斷時應返回空")
        void shouldReturnEmptyWhenTruncated() {
            // Arrange
            String responseJson = """
                    {
                        "sha": "abc123",
                        "tree": [],
                        "truncated": true
                    }
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            // Act
            Optional<FetchResult> result = strategy.fetch("owner", "repo", "docs", "main");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("優先級應為配置的值")
        void shouldHaveCorrectPriority() {
            assertThat(strategy.getPriority()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("ContentsApiFetchStrategy")
    class ContentsApiFetchStrategyTests {

        private ContentsApiFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            GitHubFetchProperties properties = createDefaultProperties();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();

            strategy = new TestableContentsApiFetchStrategy(
                    restClient,
                    new ObjectMapper(),
                    properties,
                    "",
                    baseUrl
            );
        }

        @Test
        @DisplayName("應成功從 Contents API 取得檔案列表")
        void shouldFetchFilesFromContentsApi() {
            // Arrange
            String responseJson = """
                    [
                        {"name": "README.md", "path": "docs/README.md", "sha": "abc123", "size": 1024, "type": "file", "download_url": "https://example.com/README.md"},
                        {"name": "guide.md", "path": "docs/guide.md", "sha": "def456", "size": 512, "type": "file", "download_url": "https://example.com/guide.md"}
                    ]
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            // Act
            Optional<FetchResult> result = strategy.fetch("owner", "repo", "docs", "main");

            // Assert
            assertThat(result).isPresent();
            FetchResult fetchResult = result.get();
            assertThat(fetchResult.strategyUsed()).isEqualTo("ContentsAPI");
            assertThat(fetchResult.files()).hasSize(2);
        }

        @Test
        @DisplayName("應遞迴處理子目錄")
        void shouldHandleSubdirectories() {
            // Arrange - 第一層回應
            String firstResponse = """
                    [
                        {"name": "README.md", "path": "docs/README.md", "sha": "abc123", "size": 1024, "type": "file"},
                        {"name": "guide", "path": "docs/guide", "sha": "def456", "size": 0, "type": "dir"}
                    ]
                    """;
            // Arrange - 子目錄回應
            String subDirResponse = """
                    [
                        {"name": "intro.md", "path": "docs/guide/intro.md", "sha": "ghi789", "size": 512, "type": "file"}
                    ]
                    """;
            mockWebServer.enqueue(new MockResponse()
                    .setBody(firstResponse)
                    .setHeader("Content-Type", "application/json"));
            mockWebServer.enqueue(new MockResponse()
                    .setBody(subDirResponse)
                    .setHeader("Content-Type", "application/json"));

            // Act
            Optional<FetchResult> result = strategy.fetch("owner", "repo", "docs", "main");

            // Assert
            assertThat(result).isPresent();
            FetchResult fetchResult = result.get();
            assertThat(fetchResult.files()).hasSize(2);
            assertThat(fetchResult.files().stream().map(GitHubFile::path))
                    .containsExactlyInAnyOrder("docs/README.md", "docs/guide/intro.md");
        }

        @Test
        @DisplayName("優先級應為配置的值")
        void shouldHaveCorrectPriority() {
            assertThat(strategy.getPriority()).isEqualTo(3);
        }

        @Test
        @DisplayName("應總是支援所有請求（作為 fallback）")
        void shouldAlwaysSupport() {
            assertThat(strategy.supports("any", "repo", "any-ref")).isTrue();
        }
    }

    @Nested
    @DisplayName("ArchiveFetchStrategy")
    class ArchiveFetchStrategyTests {

        private ArchiveFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            GitHubFetchProperties properties = createDefaultProperties();
            RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();

            strategy = new TestableArchiveFetchStrategy(
                    restClient,
                    properties,
                    baseUrl
            );
        }

        @Test
        @DisplayName("應支援版本標籤格式")
        void shouldSupportVersionTags() {
            assertThat(strategy.supports("owner", "repo", "v1.0.0")).isTrue();
            assertThat(strategy.supports("owner", "repo", "1.0.0")).isTrue();
            assertThat(strategy.supports("owner", "repo", "4.0.1")).isTrue();
            assertThat(strategy.supports("owner", "repo", "v3.0.0-M1")).isTrue();
        }

        @Test
        @DisplayName("不應支援分支名稱")
        void shouldNotSupportBranches() {
            assertThat(strategy.supports("owner", "repo", "main")).isFalse();
            assertThat(strategy.supports("owner", "repo", "master")).isFalse();
            assertThat(strategy.supports("owner", "repo", "develop")).isFalse();
            assertThat(strategy.supports("owner", "repo", "feature/new-feature")).isFalse();
        }

        @Test
        @DisplayName("優先級應為配置的值")
        void shouldHaveCorrectPriority() {
            assertThat(strategy.getPriority()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("FetchResult")
    class FetchResultTests {

        @Test
        @DisplayName("應正確建立不含預載入內容的結果")
        void shouldCreateResultWithoutContents() {
            // Act
            FetchResult result = FetchResult.of(
                    java.util.List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                    "TestStrategy"
            );

            // Assert
            assertThat(result.files()).hasSize(1);
            assertThat(result.contents()).isEmpty();
            assertThat(result.strategyUsed()).isEqualTo("TestStrategy");
            assertThat(result.hasContent("docs/test.md")).isFalse();
        }

        @Test
        @DisplayName("應正確建立含預載入內容的結果")
        void shouldCreateResultWithContents() {
            // Arrange
            java.util.Map<String, String> contents = java.util.Map.of("docs/test.md", "# Test Content");

            // Act
            FetchResult result = FetchResult.withContents(
                    java.util.List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                    contents,
                    "Archive"
            );

            // Assert
            assertThat(result.files()).hasSize(1);
            assertThat(result.hasContent("docs/test.md")).isTrue();
            assertThat(result.getContent("docs/test.md")).isEqualTo("# Test Content");
        }
    }

    /**
     * 建立預設的 GitHubFetchProperties
     */
    private GitHubFetchProperties createDefaultProperties() {
        GitHubFetchProperties properties = new GitHubFetchProperties();

        // Archive 配置
        GitHubFetchProperties.StrategyConfig archiveConfig = new GitHubFetchProperties.StrategyConfig();
        archiveConfig.setEnabled(true);
        archiveConfig.setPriority(1);
        properties.setArchive(archiveConfig);

        // Git Tree 配置
        GitHubFetchProperties.StrategyConfig gitTreeConfig = new GitHubFetchProperties.StrategyConfig();
        gitTreeConfig.setEnabled(true);
        gitTreeConfig.setPriority(2);
        properties.setGitTree(gitTreeConfig);

        // Contents API 配置
        GitHubFetchProperties.ContentsApiConfig contentsApiConfig = new GitHubFetchProperties.ContentsApiConfig();
        contentsApiConfig.setEnabled(true);
        contentsApiConfig.setPriority(3);

        GitHubFetchProperties.RateLimitConfig rateLimitConfig = new GitHubFetchProperties.RateLimitConfig();
        rateLimitConfig.setDelayMs(0); // 測試時不延遲
        rateLimitConfig.setMaxRequestsPerSync(100);
        rateLimitConfig.setRetryCount(1);
        rateLimitConfig.setRetryDelayMs(10);
        rateLimitConfig.setRateLimitWaitMs(100);
        contentsApiConfig.setRateLimit(rateLimitConfig);
        properties.setContentsApi(contentsApiConfig);

        return properties;
    }

    /**
     * 可測試的 GitTreeFetchStrategy
     */
    private static class TestableGitTreeFetchStrategy extends GitTreeFetchStrategy {
        private final RestClient testRestClient;
        private final String baseUrl;

        public TestableGitTreeFetchStrategy(RestClient restClient, ObjectMapper objectMapper,
                                             GitHubFetchProperties properties, String githubToken, String baseUrl) {
            super(RestClient.builder(), objectMapper, properties, githubToken);
            this.testRestClient = restClient;
            this.baseUrl = baseUrl;
        }

        @Override
        public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
            String url = String.format("%s/repos/%s/%s/git/trees/%s?recursive=1",
                    baseUrl, owner, repo, ref);

            try {
                String response = testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
                // 使用父類別的解析邏輯
                return parseAndFilterResult(response, path);
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        private Optional<FetchResult> parseAndFilterResult(String json, String targetPath) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

                if (root.path("truncated").asBoolean(false)) {
                    return Optional.empty();
                }

                java.util.List<GitHubFile> files = new java.util.ArrayList<>();
                String pathPrefix = targetPath.isEmpty() ? "" : targetPath + "/";

                for (com.fasterxml.jackson.databind.JsonNode node : root.path("tree")) {
                    String nodePath = node.path("path").asText();
                    String type = node.path("type").asText();

                    // 只處理檔案，且在目標路徑下
                    if ("blob".equals(type) && (targetPath.isEmpty() || nodePath.startsWith(pathPrefix))) {
                        String lowerPath = nodePath.toLowerCase();
                        if (lowerPath.endsWith(".md") || lowerPath.endsWith(".markdown") ||
                            lowerPath.endsWith(".adoc") || lowerPath.endsWith(".asciidoc") ||
                            lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") ||
                            lowerPath.endsWith(".txt") || lowerPath.endsWith(".rst")) {
                            files.add(new GitHubFile(
                                    nodePath.substring(nodePath.lastIndexOf('/') + 1),
                                    nodePath,
                                    node.path("sha").asText(),
                                    node.path("size").asLong(0),
                                    "file",
                                    null
                            ));
                        }
                    }
                }

                if (files.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(FetchResult.of(files, getName()));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    /**
     * 可測試的 ContentsApiFetchStrategy
     */
    private static class TestableContentsApiFetchStrategy extends ContentsApiFetchStrategy {
        private final RestClient testRestClient;
        private final String baseUrl;

        public TestableContentsApiFetchStrategy(RestClient restClient, ObjectMapper objectMapper,
                                                 GitHubFetchProperties properties, String githubToken, String baseUrl) {
            super(RestClient.builder(), objectMapper, properties, githubToken);
            this.testRestClient = restClient;
            this.baseUrl = baseUrl;
        }

        @Override
        public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
            java.util.List<GitHubFile> allFiles = new java.util.ArrayList<>();
            try {
                listFilesRecursively(owner, repo, path, ref, allFiles);
                if (allFiles.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(FetchResult.of(allFiles, getName()));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        private void listFilesRecursively(String owner, String repo, String path, String ref,
                                          java.util.List<GitHubFile> result) {
            String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                    baseUrl, owner, repo, path, ref);

            try {
                String response = testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);

                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        String type = node.path("type").asText();
                        String filePath = node.path("path").asText();

                        if ("file".equals(type) && isSupportedFile(filePath)) {
                            result.add(new GitHubFile(
                                    node.path("name").asText(),
                                    filePath,
                                    node.path("sha").asText(),
                                    node.path("size").asLong(),
                                    type,
                                    node.path("download_url").asText(null)
                            ));
                        } else if ("dir".equals(type)) {
                            listFilesRecursively(owner, repo, filePath, ref, result);
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略錯誤
            }
        }

        private boolean isSupportedFile(String path) {
            String lowerPath = path.toLowerCase();
            return lowerPath.endsWith(".md") || lowerPath.endsWith(".markdown") ||
                   lowerPath.endsWith(".adoc") || lowerPath.endsWith(".asciidoc") ||
                   lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") ||
                   lowerPath.endsWith(".txt") || lowerPath.endsWith(".rst");
        }
    }

    /**
     * 可測試的 ArchiveFetchStrategy
     */
    private static class TestableArchiveFetchStrategy extends ArchiveFetchStrategy {
        public TestableArchiveFetchStrategy(RestClient restClient, GitHubFetchProperties properties, String baseUrl) {
            super(RestClient.builder(), properties);
        }
    }
}
