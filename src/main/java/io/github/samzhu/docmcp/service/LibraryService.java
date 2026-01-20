package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.config.KnownDocsPathsProperties;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.infrastructure.github.GitHubClient;
import io.github.samzhu.docmcp.infrastructure.github.GitHubRelease;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import io.github.samzhu.docmcp.web.dto.BatchSyncRequest;
import io.github.samzhu.docmcp.web.dto.BatchSyncResponse;
import io.github.samzhu.docmcp.web.dto.GitHubReleaseDto;
import io.github.samzhu.docmcp.web.dto.GitHubReleasesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 函式庫服務
 * <p>
 * 提供函式庫相關的業務邏輯，包含列出函式庫、解析版本等功能。
 * 此服務為 MCP 工具層提供資料存取。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    /**
     * GitHub URL 解析正則表達式
     */
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/([^/]+)/([^/]+)(?:/.*)?$"
    );

    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository libraryVersionRepository;
    private final GitHubClient gitHubClient;
    private final KnownDocsPathsProperties knownDocsPathsProperties;
    private final SyncService syncService;

    public LibraryService(LibraryRepository libraryRepository,
                          LibraryVersionRepository libraryVersionRepository,
                          GitHubClient gitHubClient,
                          KnownDocsPathsProperties knownDocsPathsProperties,
                          SyncService syncService) {
        this.libraryRepository = libraryRepository;
        this.libraryVersionRepository = libraryVersionRepository;
        this.gitHubClient = gitHubClient;
        this.knownDocsPathsProperties = knownDocsPathsProperties;
        this.syncService = syncService;
    }

    /**
     * 列出所有函式庫
     * <p>
     * 若指定分類，則只回傳該分類的函式庫。
     * </p>
     *
     * @param category 分類（可選）
     * @return 函式庫列表
     */
    public List<Library> listLibraries(String category) {
        if (category != null && !category.isBlank()) {
            return libraryRepository.findByCategory(category);
        }
        return libraryRepository.findAll();
    }

    /**
     * 解析函式庫版本
     * <p>
     * 根據函式庫名稱和版本號解析出完整的函式庫和版本資訊。
     * 若未指定版本，則使用最新版本。
     * </p>
     *
     * @param name    函式庫名稱
     * @param version 版本號（可選，null 表示最新版本）
     * @return 解析結果，包含函式庫和版本資訊
     * @throws LibraryNotFoundException 若函式庫或版本不存在
     */
    public ResolvedLibrary resolveLibrary(String name, String version) {
        // 查找函式庫
        var library = libraryRepository.findByName(name)
                .orElseThrow(() -> LibraryNotFoundException.byName(name));

        // 解析版本
        LibraryVersion resolvedVersion;
        if (version != null && !version.isBlank()) {
            // 使用指定版本
            resolvedVersion = libraryVersionRepository.findByLibraryIdAndVersion(library.id(), version)
                    .orElseThrow(() -> new LibraryNotFoundException(
                            "版本 " + version + " 不存在於函式庫: " + name));
        } else {
            // 使用最新版本
            resolvedVersion = libraryVersionRepository.findLatestByLibraryId(library.id())
                    .orElseThrow(() -> new LibraryNotFoundException(
                            "函式庫 " + name + " 沒有可用的版本"));
        }

        return new ResolvedLibrary(library, resolvedVersion, resolvedVersion.version());
    }

    /**
     * 取得函式庫的所有版本
     *
     * @param libraryName 函式庫名稱
     * @return 版本列表
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    public List<LibraryVersion> getLibraryVersions(String libraryName) {
        var library = libraryRepository.findByName(libraryName)
                .orElseThrow(() -> LibraryNotFoundException.byName(libraryName));

        return libraryVersionRepository.findByLibraryId(library.id());
    }

    /**
     * 根據 ID 取得函式庫
     *
     * @param id 函式庫 ID
     * @return 函式庫
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    public Library getLibraryById(UUID id) {
        return libraryRepository.findById(id)
                .orElseThrow(() -> LibraryNotFoundException.byId(id));
    }

    /**
     * 建立新函式庫
     *
     * @param name        函式庫名稱
     * @param displayName 顯示名稱
     * @param description 描述
     * @param sourceType  來源類型
     * @param sourceUrl   來源網址
     * @param category    分類
     * @param tags        標籤列表
     * @return 建立的函式庫
     */
    @Transactional
    public Library createLibrary(String name, String displayName, String description,
                                  SourceType sourceType, String sourceUrl,
                                  String category, List<String> tags) {
        // 檢查名稱是否已存在
        if (libraryRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("函式庫名稱已存在: " + name);
        }

        Library library = Library.create(name, displayName, description,
                sourceType, sourceUrl, category, tags);
        return libraryRepository.save(library);
    }

    /**
     * 更新函式庫
     *
     * @param id          函式庫 ID
     * @param displayName 顯示名稱（null 表示不更新）
     * @param description 描述（null 表示不更新）
     * @param sourceType  來源類型（null 表示不更新）
     * @param sourceUrl   來源網址（null 表示不更新）
     * @param category    分類（null 表示不更新）
     * @param tags        標籤列表（null 表示不更新）
     * @return 更新後的函式庫
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    @Transactional
    public Library updateLibrary(UUID id, String displayName, String description,
                                  SourceType sourceType, String sourceUrl,
                                  String category, List<String> tags) {
        Library existing = getLibraryById(id);

        Library updated = new Library(
                existing.id(),
                existing.name(),
                displayName != null ? displayName : existing.displayName(),
                description != null ? description : existing.description(),
                sourceType != null ? sourceType : existing.sourceType(),
                sourceUrl != null ? sourceUrl : existing.sourceUrl(),
                category != null ? category : existing.category(),
                tags != null ? tags : existing.tags(),
                existing.createdAt(),
                null  // updatedAt 由資料庫自動處理
        );

        return libraryRepository.save(updated);
    }

    /**
     * 刪除函式庫
     *
     * @param id 函式庫 ID
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    @Transactional
    public void deleteLibrary(UUID id) {
        Library library = getLibraryById(id);
        libraryRepository.delete(library);
    }

    /**
     * 根據函式庫 ID 取得所有版本
     *
     * @param libraryId 函式庫 ID
     * @return 版本列表
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    public List<LibraryVersion> getLibraryVersionsById(UUID libraryId) {
        // 確認函式庫存在
        getLibraryById(libraryId);
        return libraryVersionRepository.findByLibraryId(libraryId);
    }

    /**
     * 根據版本 ID 取得版本
     *
     * @param versionId 版本 ID
     * @return 版本
     * @throws LibraryNotFoundException 若版本不存在
     */
    public LibraryVersion getVersionById(UUID versionId) {
        return libraryVersionRepository.findById(versionId)
                .orElseThrow(() -> new LibraryNotFoundException("版本不存在: " + versionId));
    }

    /**
     * 取得 GitHub Releases 列表
     * <p>
     * 從 GitHub API 取得指定函式庫的 Release 列表，並標記哪些版本已存在於系統中。
     * 同時自動帶入已知函式庫的預設文件路徑。
     * </p>
     *
     * @param libraryId 函式庫 ID
     * @param limit     回傳數量上限
     * @return GitHub Releases 回應（包含預設文件路徑和 Release 列表）
     * @throws LibraryNotFoundException 若函式庫不存在
     * @throws IllegalArgumentException 若函式庫的 sourceUrl 不是有效的 GitHub URL
     */
    public GitHubReleasesResponse getGitHubReleases(UUID libraryId, int limit) {
        Library library = getLibraryById(libraryId);

        // 解析 GitHub URL 取得 owner 和 repo
        GitHubInfo ghInfo = parseGitHubUrl(library.sourceUrl());
        String ownerRepo = ghInfo.owner() + "/" + ghInfo.repo();

        // 從配置取得預設文件路徑（不帶版本號，用於 API 回應的 defaultDocsPath）
        String defaultDocsPath = knownDocsPathsProperties.getDocsPath(ownerRepo);

        // 從 GitHub API 取得 Releases
        List<GitHubRelease> releases = gitHubClient.listReleases(ghInfo.owner(), ghInfo.repo());

        // 取得已存在的版本
        Set<String> existingVersions = libraryVersionRepository.findByLibraryId(libraryId)
                .stream()
                .map(LibraryVersion::version)
                .collect(Collectors.toSet());

        // 轉換為 DTO，過濾草稿和預發行版本，並限制數量
        // 每個 release 根據版本號計算對應的文件路徑
        List<GitHubReleaseDto> releaseDtos = releases.stream()
                .filter(r -> !r.draft() && !r.prerelease())
                .limit(limit)
                .map(r -> {
                    String normalizedVersion = normalizeVersion(r.tagName());
                    boolean exists = existingVersions.contains(normalizedVersion);
                    // 根據版本號取得對應的文件路徑
                    String docsPath = knownDocsPathsProperties.getDocsPath(ownerRepo, normalizedVersion);
                    return GitHubReleaseDto.from(r, exists, docsPath);
                })
                .toList();

        log.info("Retrieved {} GitHub releases for library {} ({}), defaultDocsPath={}",
                releaseDtos.size(), library.name(), ownerRepo, defaultDocsPath);

        return new GitHubReleasesResponse(defaultDocsPath, releaseDtos);
    }

    /**
     * 建立新版本
     *
     * @param libraryId   函式庫 ID
     * @param version     版本號
     * @param docsPath    文件路徑
     * @param releaseDate 發布日期
     * @param isLatest    是否為最新版本
     * @return 建立的版本
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    @Transactional
    public LibraryVersion createVersion(UUID libraryId, String version, String docsPath,
                                         LocalDate releaseDate, boolean isLatest) {
        // 確認函式庫存在
        getLibraryById(libraryId);

        // 檢查版本是否已存在
        if (libraryVersionRepository.findByLibraryIdAndVersion(libraryId, version).isPresent()) {
            throw new IllegalArgumentException("版本已存在: " + version);
        }

        // 若設為最新版本，需先將其他版本的 isLatest 設為 false
        if (isLatest) {
            libraryVersionRepository.findLatestByLibraryId(libraryId)
                    .ifPresent(existingLatest -> {
                        LibraryVersion updated = new LibraryVersion(
                                existingLatest.id(),
                                existingLatest.libraryId(),
                                existingLatest.version(),
                                false,
                                existingLatest.isLts(),
                                existingLatest.status(),
                                existingLatest.docsPath(),
                                existingLatest.releaseDate(),
                                existingLatest.createdAt(),
                                null
                        );
                        libraryVersionRepository.save(updated);
                    });
        }

        // 建立新版本
        LibraryVersion newVersion = new LibraryVersion(
                null,
                libraryId,
                version,
                isLatest,
                false,
                VersionStatus.ACTIVE,
                docsPath,
                releaseDate,
                null,
                null
        );

        return libraryVersionRepository.save(newVersion);
    }

    /**
     * 批次建立版本並同步
     * <p>
     * 為每個選中的版本建立記錄並觸發非同步同步任務。
     * </p>
     *
     * @param libraryId 函式庫 ID
     * @param request   批次同步請求
     * @return 批次同步回應
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    @Transactional
    public BatchSyncResponse batchCreateAndSync(UUID libraryId, BatchSyncRequest request) {
        Library library = getLibraryById(libraryId);

        // 解析 GitHub URL
        GitHubInfo ghInfo = parseGitHubUrl(library.sourceUrl());

        List<BatchSyncResponse.SyncedItem> syncedItems = new ArrayList<>();
        boolean isFirst = true;

        for (BatchSyncRequest.VersionSyncItem item : request.versions()) {
            // 決定文件路徑：優先使用項目指定的路徑，否則使用預設路徑
            String docsPath = item.docsPath() != null && !item.docsPath().isBlank()
                    ? item.docsPath()
                    : request.defaultDocsPath();

            // 取得發布日期（從版本號嘗試解析，或使用當天日期）
            LocalDate releaseDate = LocalDate.now();

            try {
                // 建立版本（第一個版本設為最新版本）
                LibraryVersion version = createVersion(
                        libraryId,
                        item.version(),
                        docsPath,
                        releaseDate,
                        isFirst
                );

                // 觸發非同步同步任務
                CompletableFuture<SyncHistory> syncFuture = syncService.syncFromGitHub(
                        version.id(),
                        ghInfo.owner(),
                        ghInfo.repo(),
                        docsPath,
                        item.tagName()
                );

                // 取得同步歷史 ID（非同步任務已建立，可從 future 取得結果）
                // 注意：這裡不會等待同步完成，只是取得已建立的 SyncHistory ID
                SyncHistory syncHistory = syncFuture.get();
                syncedItems.add(new BatchSyncResponse.SyncedItem(
                        version.id(),
                        item.version(),
                        syncHistory.id()
                ));

                log.info("Created version {} and started sync for library {}",
                        item.version(), library.name());

                isFirst = false;

            } catch (Exception e) {
                log.error("Failed to create and sync version {} for library {}",
                        item.version(), library.name(), e);
                // 繼續處理其他版本
            }
        }

        return BatchSyncResponse.success(syncedItems);
    }

    /**
     * 解析 GitHub URL
     *
     * @param sourceUrl GitHub URL
     * @return GitHub 資訊（owner 和 repo）
     * @throws IllegalArgumentException 若 URL 無效
     */
    private GitHubInfo parseGitHubUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("來源 URL 不可為空");
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(sourceUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("無效的 GitHub URL: " + sourceUrl);
        }

        return new GitHubInfo(matcher.group(1), matcher.group(2));
    }

    /**
     * 正規化版本號（移除 v 或 V 前綴）
     *
     * @param tagName 標籤名稱
     * @return 正規化後的版本號
     */
    private String normalizeVersion(String tagName) {
        if (tagName == null) {
            return null;
        }
        if (tagName.startsWith("v") || tagName.startsWith("V")) {
            return tagName.substring(1);
        }
        return tagName;
    }

    /**
     * GitHub 資訊
     *
     * @param owner 儲存庫擁有者
     * @param repo  儲存庫名稱
     */
    private record GitHubInfo(String owner, String repo) {}

    /**
     * 解析後的函式庫資訊
     *
     * @param library         函式庫
     * @param version         版本
     * @param resolvedVersion 解析後的版本號
     */
    public record ResolvedLibrary(
            Library library,
            LibraryVersion version,
            String resolvedVersion
    ) {}
}
