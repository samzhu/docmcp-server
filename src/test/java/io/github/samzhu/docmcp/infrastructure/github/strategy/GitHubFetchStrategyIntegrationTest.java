package io.github.samzhu.docmcp.infrastructure.github.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * GitHub Fetch 策略整合測試
 * <p>
 * 使用真實的 GitHub repository (spring-projects/spring-boot) 測試三種策略的實際運作。
 * 這些測試會發出真實的網路請求，可能因網路問題或 Rate Limit 而失敗。
 * </p>
 * <p>
 * 測試標記為 @Tag("integration")，可透過以下指令選擇性執行：
 * <pre>
 * ./gradlew test --tests "*GitHubFetchStrategyIntegrationTest*"
 * </pre>
 * </p>
 */
@DisplayName("GitHub Fetch 策略整合測試（真實網路請求）")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitHubFetchStrategyIntegrationTest {

    // 測試用的 GitHub repository 資訊
    private static final String OWNER = "spring-projects";
    private static final String REPO = "spring-boot";

    // Spring Boot 4.x 的文件路徑
    private static final String DOCS_PATH_4X = "documentation/spring-boot-docs/src/docs/antora";
    // Spring Boot 3.x 的文件路徑
    private static final String DOCS_PATH_3X = "spring-boot-project/spring-boot-docs/src/docs/antora";

    // 測試用版本標籤
    private static final String TAG_4X = "v4.0.1";
    private static final String TAG_3X = "v3.4.1";
    private static final String BRANCH_MAIN = "main";

    private static RestClient.Builder restClientBuilder;
    private static ObjectMapper objectMapper;
    private static GitHubFetchProperties properties;

    @BeforeAll
    static void setUp() {
        restClientBuilder = RestClient.builder();
        objectMapper = new ObjectMapper();
        properties = createTestProperties();
    }

    // ==================== ArchiveFetchStrategy 測試 ====================

    @Nested
    @DisplayName("ArchiveFetchStrategy 整合測試")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ArchiveFetchStrategyIntegrationTests {

        private ArchiveFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ArchiveFetchStrategy(properties);
        }

        @Test
        @Order(1)
        @DisplayName("supports() 應正確判斷版本標籤格式")
        void shouldCorrectlySupportVersionTags() {
            // v4.0.1 格式的版本標籤
            assertThat(strategy.supports(OWNER, REPO, TAG_4X))
                    .as("應支援 %s 格式", TAG_4X)
                    .isTrue();

            // v3.4.1 格式
            assertThat(strategy.supports(OWNER, REPO, TAG_3X))
                    .as("應支援 %s 格式", TAG_3X)
                    .isTrue();

            // main 分支不應支援
            assertThat(strategy.supports(OWNER, REPO, BRANCH_MAIN))
                    .as("不應支援分支名稱 %s", BRANCH_MAIN)
                    .isFalse();
        }

        @Test
        @Order(2)
        @DisplayName("fetch() 應成功下載並解壓 Spring Boot 4.x tarball")
        void shouldFetchAndExtractSpringBoot4xTarball() {
            // Given: 使用 Spring Boot 4.x 的版本和路徑
            String path = DOCS_PATH_4X;
            String ref = TAG_4X;

            System.out.printf("測試 Archive 策略: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行 fetch
            Optional<FetchResult> result = strategy.fetch(OWNER, REPO, path, ref);

            // Then: 驗證結果
            assertThat(result)
                    .as("fetch() 應返回結果")
                    .isPresent();

            FetchResult fetchResult = result.get();

            // 驗證策略名稱
            assertThat(fetchResult.strategyUsed())
                    .as("策略名稱應為 Archive")
                    .isEqualTo("Archive");

            // 驗證檔案數量
            assertThat(fetchResult.files())
                    .as("應找到文件檔案")
                    .isNotEmpty();

            // 驗證有預載入內容（Archive 策略特性）
            assertThat(fetchResult.contents())
                    .as("Archive 策略應預載入檔案內容")
                    .isNotEmpty();

            // 驗證檔案路徑格式
            for (GitHubFile file : fetchResult.files()) {
                assertThat(file.path())
                        .as("檔案路徑應以目標路徑開頭")
                        .startsWith(path);

                // 驗證對應的內容已預載入
                assertThat(fetchResult.hasContent(file.path()))
                        .as("檔案 %s 應有預載入內容", file.path())
                        .isTrue();

                String content = fetchResult.getContent(file.path());
                assertThat(content)
                        .as("檔案 %s 的內容不應為空", file.path())
                        .isNotBlank();
            }

            // 印出統計資訊
            System.out.printf("Archive 策略成功：找到 %d 個檔案，預載入 %d 個內容%n",
                    fetchResult.files().size(), fetchResult.contents().size());

            // 印出前 5 個檔案
            fetchResult.files().stream()
                    .limit(5)
                    .forEach(f -> System.out.printf("  - %s (%d bytes)%n", f.path(), f.size()));
        }

        @Test
        @Order(3)
        @DisplayName("fetch() 應成功處理 Spring Boot 3.x 的不同路徑結構")
        void shouldFetchSpringBoot3xWithDifferentPath() {
            // Given: 使用 Spring Boot 3.x 的版本和路徑
            String path = DOCS_PATH_3X;
            String ref = TAG_3X;

            System.out.printf("測試 Archive 策略（3.x）: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行 fetch
            Optional<FetchResult> result = strategy.fetch(OWNER, REPO, path, ref);

            // Then: 驗證結果
            assertThat(result)
                    .as("3.x 版本的 fetch() 應返回結果")
                    .isPresent();

            FetchResult fetchResult = result.get();

            assertThat(fetchResult.files())
                    .as("3.x 版本應找到文件檔案")
                    .isNotEmpty();

            System.out.printf("Archive 策略（3.x）成功：找到 %d 個檔案%n", fetchResult.files().size());
        }
    }

    // ==================== GitTreeFetchStrategy 測試 ====================

    @Nested
    @DisplayName("GitTreeFetchStrategy 整合測試")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GitTreeFetchStrategyIntegrationTests {

        private GitTreeFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            // 從環境變數取得 GitHub token（如果有的話）
            String githubToken = System.getenv("GITHUB_TOKEN");
            strategy = new GitTreeFetchStrategy(restClientBuilder, objectMapper, properties,
                    githubToken != null ? githubToken : "");
        }

        @Test
        @Order(1)
        @DisplayName("supports() 應支援所有 ref 格式")
        void shouldSupportAllRefFormats() {
            assertThat(strategy.supports(OWNER, REPO, TAG_4X)).isTrue();
            assertThat(strategy.supports(OWNER, REPO, TAG_3X)).isTrue();
            assertThat(strategy.supports(OWNER, REPO, BRANCH_MAIN)).isTrue();
        }

        @Test
        @Order(2)
        @DisplayName("fetch() 應成功從 Git Tree API 取得檔案列表（單次 API 呼叫）")
        void shouldFetchFilesFromGitTreeApi() {
            // Given: 使用 Spring Boot 4.x
            String path = DOCS_PATH_4X;
            String ref = TAG_4X;

            System.out.printf("測試 GitTree 策略: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行 fetch（只需 1 次 API 呼叫）
            Optional<FetchResult> result = strategy.fetch(OWNER, REPO, path, ref);

            // 如果遇到 Rate Limit，跳過此測試
            if (result.isEmpty()) {
                System.out.println("GitTree 策略返回空結果（可能因 Rate Limit 或 truncated）");
                assumeTrue(false, "GitTree API 可能遇到 Rate Limit，跳過此測試");
            }

            // Then: 驗證結果
            FetchResult fetchResult = result.get();

            // 驗證策略名稱
            assertThat(fetchResult.strategyUsed())
                    .as("策略名稱應為 GitTree")
                    .isEqualTo("GitTree");

            // 驗證檔案數量
            assertThat(fetchResult.files())
                    .as("應找到文件檔案")
                    .isNotEmpty();

            // GitTree 策略不預載入內容
            assertThat(fetchResult.contents())
                    .as("GitTree 策略不應預載入內容")
                    .isEmpty();

            // 驗證檔案格式
            for (GitHubFile file : fetchResult.files()) {
                assertThat(file.path())
                        .as("檔案路徑應以目標路徑開頭")
                        .startsWith(path);

                assertThat(file.sha())
                        .as("GitTree 策略應包含 SHA")
                        .isNotBlank();
            }

            System.out.printf("GitTree 策略成功：找到 %d 個檔案%n", fetchResult.files().size());

            // 印出前 5 個檔案
            fetchResult.files().stream()
                    .limit(5)
                    .forEach(f -> System.out.printf("  - %s (sha=%s)%n", f.path(), f.sha().substring(0, 7)));
        }

        @Test
        @Order(3)
        @DisplayName("fetch() 應支援 main 分支")
        void shouldFetchFromMainBranch() {
            // Given: 使用 main 分支
            String path = DOCS_PATH_4X;
            String ref = BRANCH_MAIN;

            System.out.printf("測試 GitTree 策略（main 分支）: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行 fetch
            Optional<FetchResult> result = strategy.fetch(OWNER, REPO, path, ref);

            // Then: 驗證結果（main 分支可能成功或因 truncated/Rate Limit 而失敗）
            if (result.isPresent()) {
                System.out.printf("GitTree 策略（main）成功：找到 %d 個檔案%n", result.get().files().size());
            } else {
                System.out.println("GitTree 策略（main）返回空結果（可能因 truncated 或 Rate Limit）");
            }
            // 這個測試主要是驗證不會拋出例外
        }
    }

    // ==================== ContentsApiFetchStrategy 測試 ====================

    @Nested
    @DisplayName("ContentsApiFetchStrategy 整合測試")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ContentsApiFetchStrategyIntegrationTests {

        // 使用淺層目錄減少 API 呼叫次數（只有 ROOT/pages 目錄，約 2-3 次 API 呼叫）
        private static final String SHALLOW_PATH = "documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages";

        private ContentsApiFetchStrategy strategy;

        @BeforeEach
        void setUp() {
            // 從環境變數取得 GitHub token（如果有的話）
            String githubToken = System.getenv("GITHUB_TOKEN");

            // 使用較短的延遲時間加速測試，限制最大請求數為 10 次
            GitHubFetchProperties testProps = createTestProperties();
            testProps.getContentsApi().getRateLimit().setDelayMs(50);
            testProps.getContentsApi().getRateLimit().setMaxRequestsPerSync(10);

            strategy = new ContentsApiFetchStrategy(restClientBuilder, objectMapper, testProps,
                    githubToken != null ? githubToken : "");
        }

        @Test
        @Order(1)
        @DisplayName("supports() 應作為 fallback 總是支援")
        void shouldAlwaysSupportAsFallback() {
            assertThat(strategy.supports(OWNER, REPO, TAG_4X)).isTrue();
            assertThat(strategy.supports(OWNER, REPO, TAG_3X)).isTrue();
            assertThat(strategy.supports(OWNER, REPO, BRANCH_MAIN)).isTrue();
            assertThat(strategy.supports("any", "repo", "any-ref")).isTrue();
        }

        @Test
        @Order(2)
        @DisplayName("fetch() 應成功從 Contents API 取得檔案（淺層目錄，約 2-3 次 API 呼叫）")
        void shouldFetchFilesFromContentsApi() {
            // Given: 使用淺層目錄減少 API 呼叫
            String path = SHALLOW_PATH;
            String ref = TAG_4X;

            System.out.printf("測試 ContentsAPI 策略（淺層）: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行 fetch
            Optional<FetchResult> result = strategy.fetch(OWNER, REPO, path, ref);

            // 如果遇到 Rate Limit，跳過此測試
            if (result.isEmpty()) {
                System.out.println("ContentsAPI 策略返回空結果（可能因 Rate Limit）");
                assumeTrue(false, "ContentsAPI 可能遇到 Rate Limit，跳過此測試");
            }

            // Then: 驗證結果
            FetchResult fetchResult = result.get();

            // 驗證策略名稱
            assertThat(fetchResult.strategyUsed())
                    .as("策略名稱應為 ContentsAPI")
                    .isEqualTo("ContentsAPI");

            // 驗證檔案數量（淺層目錄應該有一些 .adoc 檔案）
            assertThat(fetchResult.files())
                    .as("應找到文件檔案")
                    .isNotEmpty();

            // ContentsAPI 策略不預載入內容
            assertThat(fetchResult.contents())
                    .as("ContentsAPI 策略不應預載入內容")
                    .isEmpty();

            // 驗證檔案格式
            for (GitHubFile file : fetchResult.files()) {
                assertThat(file.path())
                        .as("檔案路徑應以目標路徑開頭")
                        .startsWith(path);

                assertThat(file.type())
                        .as("類型應為 file")
                        .isEqualTo("file");
            }

            System.out.printf("ContentsAPI 策略成功：找到 %d 個檔案%n", fetchResult.files().size());

            // 印出前 5 個檔案
            fetchResult.files().stream()
                    .limit(5)
                    .forEach(f -> System.out.printf("  - %s%n", f.path()));
        }
    }

    // ==================== 跨策略比較測試 ====================

    @Nested
    @DisplayName("策略比較測試")
    class StrategyComparisonTests {

        @Test
        @DisplayName("Archive 與 GitTree 策略應對相同目標返回一致的檔案列表")
        void shouldReturnConsistentResultsAcrossStrategies() {
            // Given: 建立兩種策略
            String githubToken = System.getenv("GITHUB_TOKEN");

            ArchiveFetchStrategy archiveStrategy = new ArchiveFetchStrategy(properties);
            GitTreeFetchStrategy gitTreeStrategy = new GitTreeFetchStrategy(restClientBuilder, objectMapper, properties,
                    githubToken != null ? githubToken : "");

            // 只測試 Archive 支援的版本標籤
            String path = DOCS_PATH_4X;
            String ref = TAG_4X;

            System.out.printf("比較策略結果: %s/%s path=%s ref=%s%n", OWNER, REPO, path, ref);

            // When: 執行兩種策略
            Optional<FetchResult> archiveResult = archiveStrategy.fetch(OWNER, REPO, path, ref);
            Optional<FetchResult> gitTreeResult = gitTreeStrategy.fetch(OWNER, REPO, path, ref);

            // Archive 應該總是成功（不走 API）
            assertThat(archiveResult).as("Archive 策略應成功").isPresent();

            // GitTree 可能因 Rate Limit 失敗
            if (gitTreeResult.isEmpty()) {
                System.out.println("GitTree 策略返回空結果（可能因 Rate Limit），跳過比較測試");
                assumeTrue(false, "GitTree API 可能遇到 Rate Limit，跳過此測試");
            }

            // Then: 比較檔案數量（應該相近，可能因過濾條件微小差異而略有不同）
            int archiveCount = archiveResult.get().files().size();
            int gitTreeCount = gitTreeResult.get().files().size();

            System.out.printf("Archive: %d 個檔案, GitTree: %d 個檔案%n", archiveCount, gitTreeCount);

            // 允許 10% 的差異
            double tolerance = 0.1;
            double diff = Math.abs(archiveCount - gitTreeCount) / (double) Math.max(archiveCount, gitTreeCount);

            assertThat(diff)
                    .as("兩種策略的檔案數量差異應在 %.0f%% 以內", tolerance * 100)
                    .isLessThanOrEqualTo(tolerance);

            // 驗證 Archive 有預載入內容
            assertThat(archiveResult.get().contents())
                    .as("Archive 策略應有預載入內容")
                    .isNotEmpty();

            // 驗證 GitTree 沒有預載入內容
            assertThat(gitTreeResult.get().contents())
                    .as("GitTree 策略不應有預載入內容")
                    .isEmpty();
        }
    }

    // ==================== 輔助方法 ====================

    /**
     * 建立測試用的 GitHubFetchProperties
     */
    private static GitHubFetchProperties createTestProperties() {
        GitHubFetchProperties props = new GitHubFetchProperties();

        // Archive 配置
        GitHubFetchProperties.StrategyConfig archiveConfig = new GitHubFetchProperties.StrategyConfig();
        archiveConfig.setEnabled(true);
        archiveConfig.setPriority(1);
        props.setArchive(archiveConfig);

        // Git Tree 配置
        GitHubFetchProperties.StrategyConfig gitTreeConfig = new GitHubFetchProperties.StrategyConfig();
        gitTreeConfig.setEnabled(true);
        gitTreeConfig.setPriority(2);
        props.setGitTree(gitTreeConfig);

        // Contents API 配置
        GitHubFetchProperties.ContentsApiConfig contentsApiConfig = new GitHubFetchProperties.ContentsApiConfig();
        contentsApiConfig.setEnabled(true);
        contentsApiConfig.setPriority(3);

        GitHubFetchProperties.RateLimitConfig rateLimitConfig = new GitHubFetchProperties.RateLimitConfig();
        rateLimitConfig.setDelayMs(100);
        rateLimitConfig.setMaxRequestsPerSync(100);
        rateLimitConfig.setRetryCount(2);
        rateLimitConfig.setRetryDelayMs(500);
        rateLimitConfig.setRateLimitWaitMs(5000);
        contentsApiConfig.setRateLimit(rateLimitConfig);
        props.setContentsApi(contentsApiConfig);

        return props;
    }
}
