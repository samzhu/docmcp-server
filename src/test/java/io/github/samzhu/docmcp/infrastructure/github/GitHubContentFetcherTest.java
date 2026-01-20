package io.github.samzhu.docmcp.infrastructure.github;

import io.github.samzhu.docmcp.infrastructure.github.strategy.FetchResult;
import io.github.samzhu.docmcp.infrastructure.github.strategy.GitHubFetchStrategy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GitHubContentFetcher 單元測試
 */
@DisplayName("GitHubContentFetcher")
class GitHubContentFetcherTest {

    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("應依優先級順序嘗試策略")
    void shouldTryStrategiesInPriorityOrder() {
        // Arrange - 策略 1 支援但返回空結果（失敗），策略 2 成功
        MockStrategy strategy1 = new MockStrategy(1, "Strategy1", true, Optional.empty()); // 支援但失敗
        MockStrategy strategy2 = new MockStrategy(2, "Strategy2", true,
                Optional.of(FetchResult.of(
                        List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                        "Strategy2"
                )));
        MockStrategy strategy3 = new MockStrategy(3, "Strategy3", true, Optional.empty());

        GitHubContentFetcher fetcher = new GitHubContentFetcher(
                List.of(strategy3, strategy1, strategy2), // 故意打亂順序
                RestClient.builder(),
                ""
        );

        // Act
        FetchResult result = fetcher.fetch("owner", "repo", "docs", "main");

        // Assert
        assertThat(result.strategyUsed()).isEqualTo("Strategy2");
        assertThat(strategy1.wasCalled()).isTrue();  // 策略 1 會被呼叫但返回空結果
        assertThat(strategy2.wasCalled()).isTrue();  // 策略 2 成功
        assertThat(strategy3.wasCalled()).isFalse(); // 策略 2 成功後不應呼叫策略 3
    }

    @Test
    @DisplayName("當所有策略都失敗時應拋出例外")
    void shouldThrowExceptionWhenAllStrategiesFail() {
        // Arrange - 所有策略都失敗
        MockStrategy strategy1 = new MockStrategy(1, "Strategy1", true, Optional.empty());
        MockStrategy strategy2 = new MockStrategy(2, "Strategy2", true, Optional.empty());

        GitHubContentFetcher fetcher = new GitHubContentFetcher(
                List.of(strategy1, strategy2),
                RestClient.builder(),
                ""
        );

        // Act & Assert
        assertThatThrownBy(() -> fetcher.fetch("owner", "repo", "docs", "main"))
                .isInstanceOf(GitHubContentFetcher.GitHubFetchException.class)
                .hasMessageContaining("所有策略都失敗");
    }

    @Test
    @DisplayName("不支援的策略應被跳過")
    void shouldSkipUnsupportedStrategies() {
        // Arrange - 策略 1 不支援，策略 2 支援
        MockStrategy strategy1 = new MockStrategy(1, "Strategy1", false, Optional.empty()); // 不支援
        MockStrategy strategy2 = new MockStrategy(2, "Strategy2", true,
                Optional.of(FetchResult.of(
                        List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                        "Strategy2"
                )));

        GitHubContentFetcher fetcher = new GitHubContentFetcher(
                List.of(strategy1, strategy2),
                RestClient.builder(),
                ""
        );

        // Act
        FetchResult result = fetcher.fetch("owner", "repo", "docs", "main");

        // Assert
        assertThat(result.strategyUsed()).isEqualTo("Strategy2");
        assertThat(strategy1.wasCalled()).isFalse(); // 不支援的策略不應被呼叫
        assertThat(strategy2.wasCalled()).isTrue();
    }

    @Test
    @DisplayName("應優先使用預載入內容")
    void shouldUsePreloadedContent() {
        // Arrange
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        GitHubContentFetcher fetcher = new TestableGitHubContentFetcher(
                List.of(),
                restClient,
                "",
                baseUrl
        );

        Map<String, String> preloadedContents = Map.of("docs/test.md", "# Preloaded Content");
        FetchResult fetchResult = FetchResult.withContents(
                List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                preloadedContents,
                "Archive"
        );

        // Act
        String content = fetcher.getFileContent(fetchResult, "owner", "repo", "docs/test.md", "main");

        // Assert
        assertThat(content).isEqualTo("# Preloaded Content");
    }

    @Test
    @DisplayName("當沒有預載入內容時應從 raw URL 下載")
    void shouldDownloadFromRawUrlWhenNoPreloadedContent() {
        // Arrange
        String expectedContent = "# Downloaded Content";
        mockWebServer.enqueue(new MockResponse()
                .setBody(expectedContent)
                .setHeader("Content-Type", "text/plain"));

        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        GitHubContentFetcher fetcher = new TestableGitHubContentFetcher(
                List.of(),
                restClient,
                "",
                baseUrl
        );

        FetchResult fetchResult = FetchResult.of(
                List.of(new GitHubFile("test.md", "docs/test.md", "sha", 100, "file", null)),
                "ContentsAPI"
        );

        // Act
        String content = fetcher.getFileContent(fetchResult, "owner", "repo", "docs/test.md", "main");

        // Assert
        assertThat(content).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("應返回可用策略列表")
    void shouldReturnAvailableStrategies() {
        // Arrange
        MockStrategy strategy1 = new MockStrategy(1, "Archive", true, Optional.empty());
        MockStrategy strategy2 = new MockStrategy(2, "GitTree", true, Optional.empty());
        MockStrategy strategy3 = new MockStrategy(3, "ContentsAPI", true, Optional.empty());

        GitHubContentFetcher fetcher = new GitHubContentFetcher(
                List.of(strategy2, strategy3, strategy1), // 故意打亂順序
                RestClient.builder(),
                ""
        );

        // Act
        List<String> strategies = fetcher.getAvailableStrategies();

        // Assert
        assertThat(strategies).hasSize(3);
        // 應按優先級排序
        assertThat(strategies.get(0)).contains("Archive").contains("1");
        assertThat(strategies.get(1)).contains("GitTree").contains("2");
        assertThat(strategies.get(2)).contains("ContentsAPI").contains("3");
    }

    /**
     * 模擬策略（用於測試）
     */
    private static class MockStrategy implements GitHubFetchStrategy {
        private final int priority;
        private final String name;
        private final boolean supports;
        private final Optional<FetchResult> result;
        private boolean called = false;

        public MockStrategy(int priority, String name, boolean supports, Optional<FetchResult> result) {
            this.priority = priority;
            this.name = name;
            this.supports = supports;
            this.result = result;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean supports(String owner, String repo, String ref) {
            return supports;
        }

        @Override
        public Optional<FetchResult> fetch(String owner, String repo, String path, String ref) {
            called = true;
            return result;
        }

        public boolean wasCalled() {
            return called;
        }
    }

    /**
     * 可測試的 GitHubContentFetcher
     */
    private static class TestableGitHubContentFetcher extends GitHubContentFetcher {
        private final RestClient testRestClient;
        private final String baseUrl;

        public TestableGitHubContentFetcher(List<GitHubFetchStrategy> strategies,
                                             RestClient restClient, String githubToken, String baseUrl) {
            super(strategies, RestClient.builder(), githubToken);
            this.testRestClient = restClient;
            this.baseUrl = baseUrl;
        }

        @Override
        public String downloadRawContent(String owner, String repo, String path, String ref) {
            String url = String.format("%s/%s/%s/%s/%s",
                    baseUrl, owner, repo, ref, path);

            try {
                return testRestClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                throw new GitHubFetchException("下載檔案失敗: " + path, e);
            }
        }
    }
}
