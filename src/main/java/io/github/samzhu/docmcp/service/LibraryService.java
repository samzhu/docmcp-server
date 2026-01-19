package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
