package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.infrastructure.github.GitHubContentFetcher;
import io.github.samzhu.docmcp.infrastructure.github.GitHubFile;
import io.github.samzhu.docmcp.infrastructure.github.strategy.FetchResult;
import io.github.samzhu.docmcp.infrastructure.local.LocalFileClient;
import io.github.samzhu.docmcp.infrastructure.parser.DocumentParser;
import io.github.samzhu.docmcp.infrastructure.parser.ParsedDocument;
import io.github.samzhu.docmcp.repository.CodeExampleRepository;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.SyncHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 文件同步服務
 * <p>
 * 負責從來源（GitHub、本地檔案）同步文件到資料庫。
 * 包含解析、分塊、嵌入向量生成。
 * </p>
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final GitHubContentFetcher gitHubContentFetcher;
    private final LocalFileClient localFileClient;
    private final List<DocumentParser> parsers;
    private final DocumentChunker chunker;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final SyncHistoryRepository syncHistoryRepository;

    public SyncService(GitHubContentFetcher gitHubContentFetcher,
                       LocalFileClient localFileClient,
                       List<DocumentParser> parsers,
                       DocumentChunker chunker,
                       EmbeddingService embeddingService,
                       DocumentRepository documentRepository,
                       DocumentChunkRepository chunkRepository,
                       CodeExampleRepository codeExampleRepository,
                       SyncHistoryRepository syncHistoryRepository) {
        this.gitHubContentFetcher = gitHubContentFetcher;
        this.localFileClient = localFileClient;
        this.parsers = parsers;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.codeExampleRepository = codeExampleRepository;
        this.syncHistoryRepository = syncHistoryRepository;
    }

    /**
     * 從 GitHub 同步文件（非同步執行）
     *
     * @param versionId 版本 ID
     * @param owner     GitHub 儲存庫擁有者
     * @param repo      GitHub 儲存庫名稱
     * @param docsPath  文件目錄路徑
     * @param ref       Git 參考（branch、tag 或 commit）
     * @return 同步歷史（非同步結果）
     */
    @Async
    public CompletableFuture<SyncHistory> syncFromGitHub(UUID versionId, String owner,
                                                          String repo, String docsPath, String ref) {
        log.info("Starting GitHub sync for version: {} from {}/{} path={} ref={}",
                versionId, owner, repo, docsPath, ref);

        // 檢查是否有正在執行的同步任務
        if (syncHistoryRepository.hasRunningSyncTask(versionId)) {
            log.warn("Sync task already running for version: {}", versionId);
            throw new SyncException("Already running a sync task for this version");
        }

        // 建立同步記錄
        SyncHistory syncHistory = createSyncHistory(versionId);

        try {
            // 更新狀態為執行中
            syncHistory = updateSyncStatus(syncHistory, SyncStatus.RUNNING, null);

            // 使用策略模式取得所有文件（自動選擇最佳策略）
            FetchResult fetchResult = gitHubContentFetcher.fetch(owner, repo, docsPath, ref);
            List<GitHubFile> files = fetchResult.files();
            log.info("Found {} files to sync using strategy: {}", files.size(), fetchResult.strategyUsed());

            int documentsProcessed = 0;
            int chunksCreated = 0;

            // 處理每個文件
            for (GitHubFile file : files) {
                if (file.isFile() && isSupportedFile(file.path())) {
                    try {
                        SyncResult result = processFile(versionId, owner, repo, file, ref, fetchResult);
                        documentsProcessed++;
                        chunksCreated += result.chunksCreated();
                    } catch (Exception e) {
                        log.error("Failed to process file: {}", file.path(), e);
                    }
                }
            }

            // 更新狀態為成功
            syncHistory = completeSyncHistory(syncHistory, SyncStatus.SUCCESS,
                    documentsProcessed, chunksCreated, null);

            log.info("GitHub sync completed for version: {}. Processed {} documents, created {} chunks (strategy: {})",
                    versionId, documentsProcessed, chunksCreated, fetchResult.strategyUsed());

            return CompletableFuture.completedFuture(syncHistory);

        } catch (Exception e) {
            log.error("GitHub sync failed for version: {}", versionId, e);

            // 更新狀態為失敗
            syncHistory = completeSyncHistory(syncHistory, SyncStatus.FAILED, 0, 0, e.getMessage());

            return CompletableFuture.completedFuture(syncHistory);
        }
    }

    /**
     * 取得同步狀態
     *
     * @param syncId 同步 ID
     * @return 同步歷史
     */
    public Optional<SyncHistory> getSyncStatus(UUID syncId) {
        return syncHistoryRepository.findById(syncId);
    }

    /**
     * 取得版本的最新同步記錄
     *
     * @param versionId 版本 ID
     * @return 最新的同步歷史
     */
    public Optional<SyncHistory> getLatestSyncHistory(UUID versionId) {
        return syncHistoryRepository.findLatestByVersionId(versionId);
    }

    /**
     * 取得同步歷史列表
     *
     * @param versionId 版本 ID（可選，null 表示查詢所有）
     * @param limit     結果數量上限
     * @return 同步歷史列表
     */
    public List<SyncHistory> getSyncHistory(UUID versionId, int limit) {
        if (versionId != null) {
            return syncHistoryRepository.findByVersionIdOrderByStartedAtDescLimit(versionId, limit);
        }
        return syncHistoryRepository.findAllOrderByStartedAtDesc(limit);
    }

    /**
     * 從本地文件系統同步文件（非同步執行）
     *
     * @param versionId 版本 ID
     * @param localPath 本地目錄路徑
     * @param pattern   glob 模式（如 "**\/*.md"）
     * @return 同步歷史（非同步結果）
     */
    @Async
    public CompletableFuture<SyncHistory> syncFromLocal(UUID versionId, Path localPath, String pattern) {
        log.info("Starting local sync for version: {} from path={} pattern={}",
                versionId, localPath, pattern);

        // 檢查是否有正在執行的同步任務
        if (syncHistoryRepository.hasRunningSyncTask(versionId)) {
            log.warn("Sync task already running for version: {}", versionId);
            throw new SyncException("Already running a sync task for this version");
        }

        // 建立同步記錄
        SyncHistory syncHistory = createSyncHistory(versionId);

        try {
            // 更新狀態為執行中
            syncHistory = updateSyncStatus(syncHistory, SyncStatus.RUNNING, null);

            // 讀取本地文件
            List<LocalFileClient.FileContent> files = localFileClient.readDirectory(localPath, pattern);
            log.info("Found {} files to sync from local", files.size());

            int documentsProcessed = 0;
            int chunksCreated = 0;

            // 處理每個文件
            for (LocalFileClient.FileContent file : files) {
                if (isSupportedFile(file.path())) {
                    try {
                        SyncResult result = processLocalFile(versionId, file);
                        if (result.processed()) {
                            documentsProcessed++;
                            chunksCreated += result.chunksCreated();
                        }
                    } catch (Exception e) {
                        log.error("Failed to process local file: {}", file.path(), e);
                    }
                }
            }

            // 更新狀態為成功
            syncHistory = completeSyncHistory(syncHistory, SyncStatus.SUCCESS,
                    documentsProcessed, chunksCreated, null);

            log.info("Local sync completed for version: {}. Processed {} documents, created {} chunks",
                    versionId, documentsProcessed, chunksCreated);

            return CompletableFuture.completedFuture(syncHistory);

        } catch (IOException e) {
            log.error("Local sync failed for version: {} - IO error", versionId, e);
            syncHistory = completeSyncHistory(syncHistory, SyncStatus.FAILED, 0, 0, e.getMessage());
            return CompletableFuture.completedFuture(syncHistory);

        } catch (Exception e) {
            log.error("Local sync failed for version: {}", versionId, e);
            syncHistory = completeSyncHistory(syncHistory, SyncStatus.FAILED, 0, 0, e.getMessage());
            return CompletableFuture.completedFuture(syncHistory);
        }
    }

    /**
     * 處理本地文件
     */
    @Transactional
    protected SyncResult processLocalFile(UUID versionId, LocalFileClient.FileContent file) {
        String content = file.content();
        String path = file.path();

        // 計算內容雜湊
        String contentHash = calculateHash(content);

        // 檢查是否已存在且內容相同
        Optional<Document> existingDoc = documentRepository.findByVersionIdAndPath(versionId, path);
        if (existingDoc.isPresent() && contentHash.equals(existingDoc.get().contentHash())) {
            log.debug("Skipping unchanged local file: {}", path);
            return new SyncResult(0, false);
        }

        // 找到適合的解析器
        DocumentParser parser = findParser(path);
        if (parser == null) {
            log.warn("No parser found for local file: {}", path);
            return new SyncResult(0, false);
        }

        // 解析文件
        ParsedDocument parsed = parser.parse(content, path);

        // 刪除舊資料（如果存在）
        if (existingDoc.isPresent()) {
            UUID docId = existingDoc.get().id();
            codeExampleRepository.findByDocumentId(docId)
                    .forEach(ex -> codeExampleRepository.delete(ex));
            chunkRepository.findByDocumentIdOrderByChunkIndex(docId)
                    .forEach(chunk -> chunkRepository.delete(chunk));
            documentRepository.delete(existingDoc.get());
        }

        // 儲存文件
        Document document = Document.create(versionId, parsed.title(), path,
                content, contentHash, parser.getDocType());
        document = documentRepository.save(document);
        UUID documentId = document.id();

        // 分塊並建立嵌入
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content);
        int chunksCreated = 0;

        for (DocumentChunker.ChunkResult chunkResult : chunks) {
            float[] embedding = embeddingService.embed(chunkResult.content());
            DocumentChunk chunk = DocumentChunk.create(documentId, chunkResult.index(),
                    chunkResult.content(), embedding, chunkResult.tokenCount());
            chunkRepository.save(chunk);
            chunksCreated++;
        }

        // 儲存程式碼範例
        for (ParsedDocument.CodeBlock codeBlock : parsed.codeBlocks()) {
            CodeExample example = CodeExample.create(documentId, codeBlock.language(),
                    codeBlock.code(), codeBlock.description());
            codeExampleRepository.save(example);
        }

        return new SyncResult(chunksCreated, true);
    }

    /**
     * 處理單一文件
     *
     * @param versionId   版本 ID
     * @param owner       GitHub 儲存庫擁有者
     * @param repo        GitHub 儲存庫名稱
     * @param file        檔案資訊
     * @param ref         Git 參考
     * @param fetchResult 取得結果（可能包含預載入的內容）
     * @return 同步結果
     */
    @Transactional
    protected SyncResult processFile(UUID versionId, String owner, String repo,
                                      GitHubFile file, String ref, FetchResult fetchResult) {
        // 取得文件內容（優先使用預載入內容，否則從 raw URL 下載）
        String content = gitHubContentFetcher.getFileContent(fetchResult, owner, repo, file.path(), ref);

        // 計算內容雜湊
        String contentHash = calculateHash(content);

        // 檢查是否已存在且內容相同
        Optional<Document> existingDoc = documentRepository.findByVersionIdAndPath(versionId, file.path());
        if (existingDoc.isPresent() && contentHash.equals(existingDoc.get().contentHash())) {
            log.debug("Skipping unchanged file: {}", file.path());
            return new SyncResult(0, false);
        }

        // 找到適合的解析器
        DocumentParser parser = findParser(file.path());
        if (parser == null) {
            log.warn("No parser found for file: {}", file.path());
            return new SyncResult(0, false);
        }

        // 解析文件
        ParsedDocument parsed = parser.parse(content, file.path());

        // 刪除舊資料（如果存在）
        if (existingDoc.isPresent()) {
            UUID docId = existingDoc.get().id();
            codeExampleRepository.findByDocumentId(docId)
                    .forEach(ex -> codeExampleRepository.delete(ex));
            chunkRepository.findByDocumentIdOrderByChunkIndex(docId)
                    .forEach(chunk -> chunkRepository.delete(chunk));
            documentRepository.delete(existingDoc.get());
        }

        // 儲存文件
        Document document = Document.create(versionId, parsed.title(), file.path(),
                content, contentHash, parser.getDocType());
        document = documentRepository.save(document);
        UUID documentId = document.id();

        // 分塊並建立嵌入
        List<DocumentChunker.ChunkResult> chunks = chunker.chunk(content);
        int chunksCreated = 0;

        for (DocumentChunker.ChunkResult chunkResult : chunks) {
            float[] embedding = embeddingService.embed(chunkResult.content());
            DocumentChunk chunk = DocumentChunk.create(documentId, chunkResult.index(),
                    chunkResult.content(), embedding, chunkResult.tokenCount());
            chunkRepository.save(chunk);
            chunksCreated++;
        }

        // 儲存程式碼範例
        for (ParsedDocument.CodeBlock codeBlock : parsed.codeBlocks()) {
            CodeExample example = CodeExample.create(documentId, codeBlock.language(),
                    codeBlock.code(), codeBlock.description());
            codeExampleRepository.save(example);
        }

        return new SyncResult(chunksCreated, true);
    }

    private boolean isSupportedFile(String path) {
        return parsers.stream().anyMatch(p -> p.supports(path));
    }

    private DocumentParser findParser(String path) {
        return parsers.stream()
                .filter(p -> p.supports(path))
                .findFirst()
                .orElse(null);
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    @Transactional
    protected SyncHistory createSyncHistory(UUID versionId) {
        SyncHistory history = SyncHistory.createPending(versionId);
        return syncHistoryRepository.save(history);
    }

    @Transactional
    protected SyncHistory updateSyncStatus(SyncHistory history, SyncStatus status, String errorMessage) {
        SyncHistory updated = new SyncHistory(
                history.id(),
                history.versionId(),
                status,
                history.startedAt(),
                null,
                history.documentsProcessed(),
                history.chunksCreated(),
                errorMessage,
                history.metadata()
        );
        return syncHistoryRepository.save(updated);
    }

    @Transactional
    protected SyncHistory completeSyncHistory(SyncHistory history, SyncStatus status,
                                               int documentsProcessed, int chunksCreated,
                                               String errorMessage) {
        SyncHistory updated = new SyncHistory(
                history.id(),
                history.versionId(),
                status,
                history.startedAt(),
                OffsetDateTime.now(),
                documentsProcessed,
                chunksCreated,
                errorMessage,
                Map.of()
        );
        return syncHistoryRepository.save(updated);
    }

    /**
     * 同步結果
     */
    private record SyncResult(int chunksCreated, boolean processed) {}

    /**
     * 同步例外
     */
    public static class SyncException extends RuntimeException {
        public SyncException(String message) {
            super(message);
        }

        public SyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
