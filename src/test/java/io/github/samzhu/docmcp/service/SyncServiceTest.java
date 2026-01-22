package io.github.samzhu.docmcp.service;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.infrastructure.github.GitHubContentFetcher;
import io.github.samzhu.docmcp.infrastructure.github.strategy.FetchResult;
import io.github.samzhu.docmcp.infrastructure.local.LocalFileClient;
import io.github.samzhu.docmcp.infrastructure.parser.DocumentParser;
import io.github.samzhu.docmcp.infrastructure.vectorstore.DocumentChunkConverter;
import io.github.samzhu.docmcp.repository.CodeExampleRepository;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.SyncHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SyncService 單元測試
 * <p>
 * 測試文件同步服務的核心功能：同步 GitHub 文件、取得同步狀態、同步歷史等。
 * 使用 Mockito 模擬所有外部依賴。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SyncService")
class SyncServiceTest {

    @Mock
    private IdService idService;

    @Mock
    private GitHubContentFetcher gitHubContentFetcher;

    @Mock
    private LocalFileClient localFileClient;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private DocumentChunker chunker;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentChunkConverter chunkConverter;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private CodeExampleRepository codeExampleRepository;

    @Mock
    private SyncHistoryRepository syncHistoryRepository;

    private SyncService syncService;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        // 建立包含一個 parser 的列表
        List<DocumentParser> parsers = List.of(documentParser);

        // Mock IdService 回傳隨機 ID
        when(idService.generateId()).thenAnswer(inv -> randomId());

        syncService = new SyncService(
                idService,
                gitHubContentFetcher,
                localFileClient,
                parsers,
                chunker,
                vectorStore,
                chunkConverter,
                documentRepository,
                chunkRepository,
                codeExampleRepository,
                syncHistoryRepository
        );
    }

    // ==================== syncFromGitHub() 方法測試 ====================

    @Nested
    @DisplayName("syncFromGitHub() 方法")
    class SyncFromGitHubTests {

        @Test
        @DisplayName("正常同步 GitHub 文件")
        void shouldSyncSuccessfully_whenGitHubFilesExist() throws ExecutionException, InterruptedException {
            // Given - 準備測試資料
            String versionId = randomId();
            String owner = "spring-projects";
            String repo = "spring-boot";
            String docsPath = "docs";
            String ref = "main";

            // Mock 無進行中的同步任務
            when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(false);

            // Mock 建立同步歷史（save 後會呼叫 findById 取得 DB 生成的 startedAt）
            SyncHistory pendingHistory = createSyncHistory(versionId, SyncStatus.PENDING);
            SyncHistory runningHistory = createSyncHistory(versionId, SyncStatus.RUNNING);
            SyncHistory successHistory = createSyncHistory(versionId, SyncStatus.SUCCESS);

            when(syncHistoryRepository.save(any(SyncHistory.class)))
                    .thenReturn(pendingHistory)
                    .thenReturn(runningHistory)
                    .thenReturn(successHistory);

            // Mock findById（save 後會呼叫 findById 取得 DB 生成的 createdAt/updatedAt）
            when(syncHistoryRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(pendingHistory))
                    .thenReturn(Optional.of(runningHistory))
                    .thenReturn(Optional.of(successHistory));

            // Mock GitHub 取得文件（沒有文件，簡化測試）
            FetchResult fetchResult = new FetchResult(List.of(), Map.of(), "API_TREE");
            when(gitHubContentFetcher.fetch(owner, repo, docsPath, ref)).thenReturn(fetchResult);

            // When - 執行同步
            CompletableFuture<SyncHistory> future = syncService.syncFromGitHub(versionId, owner, repo, docsPath, ref);
            SyncHistory result = future.get();

            // Then - 驗證結果
            assertThat(result).isNotNull();
            verify(gitHubContentFetcher).fetch(owner, repo, docsPath, ref);
            verify(syncHistoryRepository).hasRunningSyncTask(versionId);
        }

        @Test
        @DisplayName("已有進行中的同步任務時拋出例外")
        void shouldThrowException_whenSyncAlreadyRunning() {
            // Given - 準備測試資料
            String versionId = randomId();
            String owner = "spring-projects";
            String repo = "spring-boot";
            String docsPath = "docs";
            String ref = "main";

            // Mock 有進行中的同步任務
            when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(true);

            // When & Then - 應該拋出 SyncException
            assertThatThrownBy(() -> syncService.syncFromGitHub(versionId, owner, repo, docsPath, ref))
                    .isInstanceOf(SyncService.SyncException.class)
                    .hasMessageContaining("Already running a sync task");
        }

        @Test
        @DisplayName("GitHub API 失敗時記錄錯誤狀態")
        void shouldRecordFailedStatus_whenGitHubApiFails() throws ExecutionException, InterruptedException {
            // Given - 準備測試資料
            String versionId = randomId();
            String owner = "nonexistent";
            String repo = "repo";
            String docsPath = "docs";
            String ref = "main";

            // Mock 無進行中的同步任務
            when(syncHistoryRepository.hasRunningSyncTask(versionId)).thenReturn(false);

            // Mock 建立同步歷史（save 後會呼叫 findById 取得 DB 生成的 startedAt）
            SyncHistory pendingHistory = createSyncHistory(versionId, SyncStatus.PENDING);
            SyncHistory runningHistory = createSyncHistory(versionId, SyncStatus.RUNNING);
            SyncHistory failedHistory = createSyncHistory(versionId, SyncStatus.FAILED);

            when(syncHistoryRepository.save(any(SyncHistory.class)))
                    .thenReturn(pendingHistory)
                    .thenReturn(runningHistory)
                    .thenReturn(failedHistory);

            // Mock findById（save 後會呼叫 findById 取得 DB 生成的 createdAt/updatedAt）
            when(syncHistoryRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(pendingHistory))
                    .thenReturn(Optional.of(runningHistory))
                    .thenReturn(Optional.of(failedHistory));

            // Mock GitHub API 失敗
            when(gitHubContentFetcher.fetch(owner, repo, docsPath, ref))
                    .thenThrow(new RuntimeException("GitHub API error: 404 Not Found"));

            // When - 執行同步
            CompletableFuture<SyncHistory> future = syncService.syncFromGitHub(versionId, owner, repo, docsPath, ref);
            SyncHistory result = future.get();

            // Then - 驗證結果（應該記錄失敗狀態而非拋出例外）
            assertThat(result).isNotNull();
            verify(syncHistoryRepository).hasRunningSyncTask(versionId);
        }
    }

    // ==================== getSyncStatus() 方法測試 ====================

    @Nested
    @DisplayName("getSyncStatus() 方法")
    class GetSyncStatusTests {

        @Test
        @DisplayName("正常取得同步狀態")
        void shouldReturnSyncStatus_whenSyncExists() {
            // Given - 準備測試資料
            String syncId = randomId();
            SyncHistory syncHistory = createSyncHistory(randomId(), SyncStatus.SUCCESS);

            when(syncHistoryRepository.findById(syncId)).thenReturn(Optional.of(syncHistory));

            // When - 取得同步狀態
            Optional<SyncHistory> result = syncService.getSyncStatus(syncId);

            // Then - 驗證結果
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(SyncStatus.SUCCESS);
            verify(syncHistoryRepository).findById(syncId);
        }

        @Test
        @DisplayName("同步不存在時返回 Optional.empty()")
        void shouldReturnEmpty_whenSyncNotExists() {
            // Given - 準備測試資料
            String syncId = randomId();

            when(syncHistoryRepository.findById(syncId)).thenReturn(Optional.empty());

            // When - 取得同步狀態
            Optional<SyncHistory> result = syncService.getSyncStatus(syncId);

            // Then - 驗證結果
            assertThat(result).isEmpty();
            verify(syncHistoryRepository).findById(syncId);
        }
    }

    // ==================== getLatestSyncHistory() 方法測試 ====================

    @Nested
    @DisplayName("getLatestSyncHistory() 方法")
    class GetLatestSyncHistoryTests {

        @Test
        @DisplayName("正常取得版本的最新同步記錄")
        void shouldReturnLatestSyncHistory_whenExists() {
            // Given - 準備測試資料
            String versionId = randomId();
            SyncHistory latestHistory = createSyncHistory(versionId, SyncStatus.SUCCESS);

            when(syncHistoryRepository.findLatestByVersionId(versionId))
                    .thenReturn(Optional.of(latestHistory));

            // When - 取得最新同步記錄
            Optional<SyncHistory> result = syncService.getLatestSyncHistory(versionId);

            // Then - 驗證結果
            assertThat(result).isPresent();
            verify(syncHistoryRepository).findLatestByVersionId(versionId);
        }

        @Test
        @DisplayName("版本無同步記錄時返回 Optional.empty()")
        void shouldReturnEmpty_whenNoSyncHistory() {
            // Given - 準備測試資料
            String versionId = randomId();

            when(syncHistoryRepository.findLatestByVersionId(versionId))
                    .thenReturn(Optional.empty());

            // When - 取得最新同步記錄
            Optional<SyncHistory> result = syncService.getLatestSyncHistory(versionId);

            // Then - 驗證結果
            assertThat(result).isEmpty();
            verify(syncHistoryRepository).findLatestByVersionId(versionId);
        }
    }

    // ==================== getSyncHistory() 方法測試 ====================

    @Nested
    @DisplayName("getSyncHistory() 方法")
    class GetSyncHistoryTests {

        @Test
        @DisplayName("取得指定版本的同步歷史列表")
        void shouldReturnSyncHistoryList_forSpecificVersion() {
            // Given - 準備測試資料
            String versionId = randomId();
            int limit = 10;
            List<SyncHistory> historyList = List.of(
                    createSyncHistory(versionId, SyncStatus.SUCCESS),
                    createSyncHistory(versionId, SyncStatus.FAILED)
            );

            when(syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, limit))
                    .thenReturn(historyList);

            // When - 取得同步歷史
            List<SyncHistory> result = syncService.getSyncHistory(versionId, limit);

            // Then - 驗證結果
            assertThat(result).hasSize(2);
            verify(syncHistoryRepository).findByVersionIdOrderByStartedAtDescLimit(versionId, limit);
        }

        @Test
        @DisplayName("版本為 null 時取得所有同步歷史")
        void shouldReturnAllSyncHistory_whenVersionIdIsNull() {
            // Given - 準備測試資料
            int limit = 10;
            List<SyncHistory> historyList = List.of(
                    createSyncHistory(randomId(), SyncStatus.SUCCESS),
                    createSyncHistory(randomId(), SyncStatus.RUNNING)
            );

            when(syncHistoryRepository.findAllOrderByStartedAtDesc(limit))
                    .thenReturn(historyList);

            // When - 取得同步歷史（不指定版本）
            List<SyncHistory> result = syncService.getSyncHistory(null, limit);

            // Then - 驗證結果
            assertThat(result).hasSize(2);
            verify(syncHistoryRepository).findAllOrderByStartedAtDesc(limit);
        }
    }

    // ==================== 輔助方法 ====================

    /**
     * 建立測試用的同步歷史記錄
     */
    private SyncHistory createSyncHistory(String versionId, SyncStatus status) {
        var now = OffsetDateTime.now();
        return new SyncHistory(
                randomId(),
                versionId,
                status,
                now,
                status == SyncStatus.SUCCESS || status == SyncStatus.FAILED ? now : null,
                0,
                0,
                status == SyncStatus.FAILED ? "Test error" : null,
                Map.of(),
                null,  // version
                now,   // createdAt
                now    // updatedAt
        );
    }
}
