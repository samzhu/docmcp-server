package io.github.samzhu.docmcp.service;

import io.github.samzhu.docmcp.domain.exception.DocumentNotFoundException;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.DocumentChunk;
import io.github.samzhu.docmcp.repository.CodeExampleRepository;
import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件服務
 * <p>
 * 提供文件內容檢索和程式碼範例查詢功能。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final CodeExampleRepository codeExampleRepository;
    private final LibraryVersionRepository versionRepository;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           CodeExampleRepository codeExampleRepository,
                           LibraryVersionRepository versionRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.codeExampleRepository = codeExampleRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * 取得文件完整內容
     *
     * @param documentId 文件 ID
     * @return 文件內容，包含文件資訊和區塊
     * @throws DocumentNotFoundException 若文件不存在
     */
    public DocumentContent getDocumentContent(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> DocumentNotFoundException.byId(documentId));

        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        List<CodeExample> codeExamples = codeExampleRepository.findByDocumentId(documentId);

        return new DocumentContent(document, chunks, codeExamples);
    }

    /**
     * 依路徑取得文件
     *
     * @param versionId 版本 ID
     * @param path      文件路徑
     * @return 文件（若存在）
     */
    public Optional<Document> getDocumentByPath(UUID versionId, String path) {
        return documentRepository.findByVersionIdAndPath(versionId, path);
    }

    /**
     * 取得文件（僅文件本身，不含區塊和程式碼範例）
     *
     * @param documentId 文件 ID
     * @return 文件（若存在）
     */
    public Optional<Document> getDocument(UUID documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * 取得程式碼範例
     *
     * @param libraryId 函式庫 ID
     * @param version   版本（可選）
     * @param language  程式語言篩選（可選）
     * @param limit     結果數量上限
     * @return 程式碼範例列表
     */
    public List<CodeExample> getCodeExamples(UUID libraryId, String version,
                                              String language, int limit) {
        // 如果沒有指定版本，使用最新版本
        String resolvedVersion = version;
        if (version == null || version.isBlank()) {
            resolvedVersion = versionRepository.findLatestByLibraryId(libraryId)
                    .map(v -> v.version())
                    .orElse(null);
        }

        return codeExampleRepository.findByLibraryAndLanguage(libraryId, resolvedVersion, language, limit);
    }

    /**
     * 取得指定函式庫中所有可用的程式語言
     *
     * @param libraryId 函式庫 ID
     * @param version   版本（可選）
     * @return 程式語言列表
     */
    public List<String> getAvailableLanguages(UUID libraryId, String version) {
        return codeExampleRepository.findDistinctLanguagesByLibrary(libraryId, version);
    }

    /**
     * 文件內容結果
     *
     * @param document     文件
     * @param chunks       文件區塊列表
     * @param codeExamples 程式碼範例列表
     */
    public record DocumentContent(
            Document document,
            List<DocumentChunk> chunks,
            List<CodeExample> codeExamples
    ) {}
}
