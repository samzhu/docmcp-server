package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository libraryVersionRepository;

    public LibraryService(LibraryRepository libraryRepository,
                          LibraryVersionRepository libraryVersionRepository) {
        this.libraryRepository = libraryRepository;
        this.libraryVersionRepository = libraryVersionRepository;
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
