package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 版本管理服務
 * <p>
 * 提供函式庫版本的 CRUD 操作及版本解析功能。
 * 從 LibraryService 重構抽取，專責處理版本相關邏輯。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    private final IdService idService;
    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository versionRepository;

    public VersionService(IdService idService,
                          LibraryRepository libraryRepository,
                          LibraryVersionRepository versionRepository) {
        this.idService = idService;
        this.libraryRepository = libraryRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * 取得函式庫的所有版本
     *
     * @param libraryName 函式庫名稱
     * @return 版本列表
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    public List<LibraryVersion> getVersionsByLibraryName(String libraryName) {
        var library = libraryRepository.findByName(libraryName)
                .orElseThrow(() -> LibraryNotFoundException.byName(libraryName));
        return versionRepository.findByLibraryId(library.getId());
    }

    /**
     * 根據函式庫 ID 取得所有版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return 版本列表
     * @throws LibraryNotFoundException 若函式庫不存在
     */
    public List<LibraryVersion> getVersionsByLibraryId(String libraryId) {
        // 確認函式庫存在
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> LibraryNotFoundException.byId(libraryId));
        return versionRepository.findByLibraryId(libraryId);
    }

    /**
     * 根據版本 ID 取得版本
     *
     * @param versionId 版本 ID（TSID 格式）
     * @return 版本
     * @throws LibraryNotFoundException 若版本不存在
     */
    public LibraryVersion getVersionById(String versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new LibraryNotFoundException("版本不存在: " + versionId));
    }

    /**
     * 取得函式庫的最新版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return 最新版本（若存在）
     */
    public Optional<LibraryVersion> getLatestVersion(String libraryId) {
        return versionRepository.findLatestByLibraryId(libraryId);
    }

    /**
     * 取得函式庫的特定版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本號
     * @return 版本（若存在）
     */
    public Optional<LibraryVersion> getVersion(String libraryId, String version) {
        return versionRepository.findByLibraryIdAndVersion(libraryId, version);
    }

    /**
     * 解析版本
     * <p>
     * 若指定版本號，則返回該版本；否則返回最新版本。
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本號（可選，null 表示最新版本）
     * @return 解析後的版本
     * @throws LibraryNotFoundException 若版本不存在
     */
    public LibraryVersion resolveVersion(String libraryId, String version) {
        if (version != null && !version.isBlank()) {
            return versionRepository.findByLibraryIdAndVersion(libraryId, version)
                    .orElseThrow(() -> new LibraryNotFoundException(
                            "版本 " + version + " 不存在於函式庫 ID: " + libraryId));
        } else {
            return versionRepository.findLatestByLibraryId(libraryId)
                    .orElseThrow(() -> new LibraryNotFoundException(
                            "函式庫 ID " + libraryId + " 沒有可用的版本"));
        }
    }

    /**
     * 建立新版本
     *
     * @param libraryId   函式庫 ID（TSID 格式）
     * @param version     版本號
     * @param docsPath    文件路徑
     * @param releaseDate 發布日期
     * @param isLatest    是否為最新版本
     * @return 建立的版本
     * @throws LibraryNotFoundException 若函式庫不存在
     * @throws IllegalArgumentException 若版本已存在
     */
    @Transactional
    public LibraryVersion createVersion(String libraryId, String version, String docsPath,
                                         LocalDate releaseDate, boolean isLatest) {
        // 確認函式庫存在
        libraryRepository.findById(libraryId)
                .orElseThrow(() -> LibraryNotFoundException.byId(libraryId));

        // 檢查版本是否已存在
        if (versionRepository.findByLibraryIdAndVersion(libraryId, version).isPresent()) {
            throw new IllegalArgumentException("版本已存在: " + version);
        }

        // 若設為最新版本，需先將其他版本的 isLatest 設為 false
        if (isLatest) {
            clearLatestFlag(libraryId);
        }

        // 使用 IdService 生成 TSID
        String id = idService.generateId();

        // 建立新版本（entityVersion = null 表示新實體）
        LibraryVersion newVersion = new LibraryVersion(
                id,
                libraryId,
                version,
                isLatest,
                false,
                VersionStatus.ACTIVE,
                docsPath,
                releaseDate,
                null,  // entityVersion = null 表示新實體
                null,
                null
        );

        LibraryVersion saved = versionRepository.save(newVersion);
        log.info("建立新版本: {} (libraryId={})", version, libraryId);

        return saved;
    }

    /**
     * 更新版本資訊
     *
     * @param versionId 版本 ID（TSID 格式）
     * @param docsPath  文件路徑（null 表示不更新）
     * @param status    版本狀態（null 表示不更新）
     * @param isLatest  是否為最新版本（null 表示不更新）
     * @param isLts     是否為 LTS 版本（null 表示不更新）
     * @return 更新後的版本
     * @throws LibraryNotFoundException 若版本不存在
     */
    @Transactional
    public LibraryVersion updateVersion(String versionId, String docsPath, VersionStatus status,
                                         Boolean isLatest, Boolean isLts) {
        LibraryVersion existing = getVersionById(versionId);

        // 若設為最新版本，需先清除其他版本的 isLatest 標記
        boolean newIsLatest = isLatest != null ? isLatest : existing.getIsLatest();
        if (isLatest != null && isLatest && !existing.getIsLatest()) {
            clearLatestFlag(existing.getLibraryId());
        }

        LibraryVersion updated = new LibraryVersion(
                existing.getId(),
                existing.getLibraryId(),
                existing.getVersion(),
                newIsLatest,
                isLts != null ? isLts : existing.getIsLts(),
                status != null ? status : existing.getStatus(),
                docsPath != null ? docsPath : existing.getDocsPath(),
                existing.getReleaseDate(),
                existing.getEntityVersion(),  // 保留 entityVersion 以進行樂觀鎖定
                existing.getCreatedAt(),
                null
        );

        return versionRepository.save(updated);
    }

    /**
     * 刪除版本
     *
     * @param versionId 版本 ID（TSID 格式）
     * @throws LibraryNotFoundException 若版本不存在
     */
    @Transactional
    public void deleteVersion(String versionId) {
        LibraryVersion version = getVersionById(versionId);
        versionRepository.delete(version);
        log.info("刪除版本: {} (id={})", version.getVersion(), versionId);
    }

    /**
     * 設定最新版本
     *
     * @param versionId 要設為最新的版本 ID（TSID 格式）
     * @return 更新後的版本
     * @throws LibraryNotFoundException 若版本不存在
     */
    @Transactional
    public LibraryVersion setLatestVersion(String versionId) {
        LibraryVersion version = getVersionById(versionId);

        // 清除同函式庫其他版本的 isLatest 標記
        clearLatestFlag(version.getLibraryId());

        // 設定此版本為最新
        LibraryVersion updated = new LibraryVersion(
                version.getId(),
                version.getLibraryId(),
                version.getVersion(),
                true,
                version.getIsLts(),
                version.getStatus(),
                version.getDocsPath(),
                version.getReleaseDate(),
                version.getEntityVersion(),  // 保留 entityVersion 以進行樂觀鎖定
                version.getCreatedAt(),
                null
        );

        return versionRepository.save(updated);
    }

    /**
     * 取得 LTS 版本列表
     *
     * @return LTS 版本列表
     */
    public List<LibraryVersion> getLtsVersions() {
        return versionRepository.findAllLts();
    }

    /**
     * 取得函式庫的 LTS 版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return LTS 版本（若存在）
     */
    public Optional<LibraryVersion> getLtsVersion(String libraryId) {
        return versionRepository.findLtsByLibraryId(libraryId);
    }

    /**
     * 正規化版本號（移除 v 或 V 前綴）
     *
     * @param tagName 標籤名稱
     * @return 正規化後的版本號
     */
    public String normalizeVersion(String tagName) {
        if (tagName == null) {
            return null;
        }
        if (tagName.startsWith("v") || tagName.startsWith("V")) {
            return tagName.substring(1);
        }
        return tagName;
    }

    /**
     * 清除函式庫所有版本的 isLatest 標記
     */
    private void clearLatestFlag(String libraryId) {
        versionRepository.findLatestByLibraryId(libraryId)
                .ifPresent(existingLatest -> {
                    LibraryVersion updated = new LibraryVersion(
                            existingLatest.getId(),
                            existingLatest.getLibraryId(),
                            existingLatest.getVersion(),
                            false,
                            existingLatest.getIsLts(),
                            existingLatest.getStatus(),
                            existingLatest.getDocsPath(),
                            existingLatest.getReleaseDate(),
                            existingLatest.getEntityVersion(),  // 保留 entityVersion 以進行樂觀鎖定
                            existingLatest.getCreatedAt(),
                            null
                    );
                    versionRepository.save(updated);
                });
    }
}
