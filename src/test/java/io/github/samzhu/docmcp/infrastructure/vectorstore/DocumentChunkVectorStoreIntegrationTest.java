package io.github.samzhu.docmcp.infrastructure.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunkVectorStore 整合測試
 * <p>
 * 使用 Testcontainers 提供的 PostgreSQL + pgvector 進行真實資料庫測試。
 * 參考 Spring AI PgVectorStoreIT 測試模式。
 * </p>
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
@DisplayName("DocumentChunkVectorStore 整合測試")
@Tag("integration")
class DocumentChunkVectorStoreIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ObjectMapper objectMapper;

    private DocumentChunkVectorStore vectorStore;

    // 測試用向量維度（與 MockEmbeddingModel 一致）
    private static final int DIMENSIONS = 768;

    // 測試用的 Library 和 Version ID（每個測試會重新建立）
    private UUID testLibraryId;
    private UUID testVersionId;

    @BeforeEach
    void setUp() {
        // 建立 VectorStore 實例
        vectorStore = new DocumentChunkVectorStore(
                jdbcTemplate,
                embeddingModel,
                objectMapper,
                DIMENSIONS
        );

        // 清理測試資料（按照外鍵順序）
        jdbcTemplate.execute("DELETE FROM document_chunks");
        jdbcTemplate.execute("DELETE FROM code_examples");
        jdbcTemplate.execute("DELETE FROM documents");
        jdbcTemplate.execute("DELETE FROM sync_history");
        jdbcTemplate.execute("DELETE FROM library_versions");
        jdbcTemplate.execute("DELETE FROM libraries");

        // 建立測試所需的父記錄
        testLibraryId = createTestLibrary();
        testVersionId = createTestVersion(testLibraryId);
    }

    // ==================== add() 方法整合測試 ====================

    @Nested
    @DisplayName("add() 整合測試")
    class AddIntegrationTests {

        @Test
        @DisplayName("新增單一文件到資料庫")
        void shouldAddSingleDocument() {
            // Given - 建立父記錄並準備測試文件
            UUID documentId = createTestDocument(testVersionId, "/test/doc1.md");
            String chunkId = UUID.randomUUID().toString();

            Document document = Document.builder()
                    .id(chunkId)
                    .text("Spring Boot 是一個強大的 Java 框架")
                    .metadata(Map.of(
                            DocumentChunkVectorStore.METADATA_VERSION_ID, testVersionId.toString(),
                            DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                            DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 0,
                            DocumentChunkVectorStore.METADATA_TOKEN_COUNT, 15,
                            DocumentChunkVectorStore.METADATA_DOCUMENT_TITLE, "Spring Boot 入門"
                    ))
                    .build();

            // When - 新增文件
            vectorStore.add(List.of(document));

            // Then - 驗證資料已寫入資料庫
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE id = ?::uuid",
                    Integer.class,
                    chunkId
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("批次新增多個文件")
        void shouldAddMultipleDocuments() {
            // Given - 建立父記錄並準備多個測試文件
            UUID documentId = createTestDocument(testVersionId, "/test/doc2.md");

            List<Document> documents = List.of(
                    createChunkDocument(testVersionId, documentId, 0, "第一個區塊內容"),
                    createChunkDocument(testVersionId, documentId, 1, "第二個區塊內容"),
                    createChunkDocument(testVersionId, documentId, 2, "第三個區塊內容")
            );

            // When - 批次新增
            vectorStore.add(documents);

            // Then - 驗證所有文件已寫入
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?::uuid",
                    Integer.class,
                    documentId
            );
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("更新已存在的文件（UPSERT）")
        void shouldUpsertExistingDocument() {
            // Given - 建立父記錄並先新增一個文件
            UUID documentId = createTestDocument(testVersionId, "/test/doc3.md");
            String chunkId = UUID.randomUUID().toString();

            Document originalDoc = Document.builder()
                    .id(chunkId)
                    .text("原始內容")
                    .metadata(Map.of(
                            DocumentChunkVectorStore.METADATA_VERSION_ID, testVersionId.toString(),
                            DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                            DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 0,
                            DocumentChunkVectorStore.METADATA_TOKEN_COUNT, 5
                    ))
                    .build();

            vectorStore.add(List.of(originalDoc));

            // When - 使用相同 ID 新增更新後的文件
            Document updatedDoc = Document.builder()
                    .id(chunkId)
                    .text("更新後的內容")
                    .metadata(Map.of(
                            DocumentChunkVectorStore.METADATA_VERSION_ID, testVersionId.toString(),
                            DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                            DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 0,
                            DocumentChunkVectorStore.METADATA_TOKEN_COUNT, 8
                    ))
                    .build();

            vectorStore.add(List.of(updatedDoc));

            // Then - 驗證內容已更新（只有一筆記錄）
            String content = jdbcTemplate.queryForObject(
                    "SELECT content FROM document_chunks WHERE id = ?::uuid",
                    String.class,
                    chunkId
            );
            assertThat(content).isEqualTo("更新後的內容");

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE id = ?::uuid",
                    Integer.class,
                    chunkId
            );
            assertThat(count).isEqualTo(1);
        }
    }

    // ==================== similaritySearch() 方法整合測試 ====================

    @Nested
    @DisplayName("similaritySearch() 整合測試")
    class SimilaritySearchIntegrationTests {

        @Test
        @DisplayName("搜尋並返回結果")
        void shouldSearchAndReturnResults() {
            // Given - 建立父記錄並準備測試資料
            UUID documentId = createTestDocument(testVersionId, "/test/search1.md");

            List<Document> documents = List.of(
                    createChunkDocument(testVersionId, documentId, 0, "Spring Boot 框架介紹"),
                    createChunkDocument(testVersionId, documentId, 1, "PostgreSQL 資料庫設定"),
                    createChunkDocument(testVersionId, documentId, 2, "Docker 容器化部署")
            );
            vectorStore.add(documents);

            // When - 執行搜尋
            SearchRequest request = SearchRequest.builder()
                    .query("Spring")
                    .topK(5)
                    .similarityThreshold(0.0) // 降低閾值以確保返回結果
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            // Then - 驗證返回結果
            // 注意：由於 MockEmbeddingModel 返回固定向量，所有文件的相似度相同
            assertThat(results).isNotEmpty();
            assertThat(results.size()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("使用 filter 過濾 versionId")
        void shouldFilterByVersionId() {
            // Given - 建立兩個版本的測試資料
            UUID versionId2 = createTestVersion(testLibraryId, "2.0.0");

            UUID doc1 = createTestDocument(testVersionId, "/test/v1/doc1.md");
            UUID doc2 = createTestDocument(testVersionId, "/test/v1/doc2.md");
            UUID doc3 = createTestDocument(versionId2, "/test/v2/doc1.md");

            vectorStore.add(List.of(
                    createChunkDocument(testVersionId, doc1, 0, "版本 1 的內容"),
                    createChunkDocument(testVersionId, doc2, 0, "版本 1 的更多內容")
            ));
            vectorStore.add(List.of(
                    createChunkDocument(versionId2, doc3, 0, "版本 2 的內容")
            ));

            // When - 搜尋並過濾特定版本
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                    .query("內容")
                    .topK(10)
                    .similarityThreshold(0.0)
                    .filterExpression(filterBuilder.eq(
                            DocumentChunkVectorStore.METADATA_VERSION_ID,
                            testVersionId.toString()
                    ).build())
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            // Then - 只應返回版本 1 的文件
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(doc ->
                    testVersionId.toString().equals(doc.getMetadata().get(DocumentChunkVectorStore.METADATA_VERSION_ID))
            );
        }

        @Test
        @DisplayName("限制返回結果數量（topK）")
        void shouldLimitResultsByTopK() {
            // Given - 建立超過 topK 數量的測試資料
            UUID documentId = createTestDocument(testVersionId, "/test/topk.md");

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                documents.add(createChunkDocument(testVersionId, documentId, i, "文件內容 " + i));
            }
            vectorStore.add(documents);

            // When - 搜尋並限制返回數量
            SearchRequest request = SearchRequest.builder()
                    .query("文件")
                    .topK(3)
                    .similarityThreshold(0.0)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            // Then - 最多返回 3 個結果
            assertThat(results).hasSizeLessThanOrEqualTo(3);
        }
    }

    // ==================== delete() 方法整合測試 ====================

    @Nested
    @DisplayName("delete() 整合測試")
    class DeleteIntegrationTests {

        @Test
        @DisplayName("依 ID 列表刪除文件")
        void shouldDeleteByIdList() {
            // Given - 建立父記錄並準備測試資料
            UUID documentId = createTestDocument(testVersionId, "/test/delete1.md");
            String chunkId1 = UUID.randomUUID().toString();
            String chunkId2 = UUID.randomUUID().toString();

            vectorStore.add(List.of(
                    Document.builder()
                            .id(chunkId1)
                            .text("要刪除的文件 1")
                            .metadata(Map.of(
                                    DocumentChunkVectorStore.METADATA_VERSION_ID, testVersionId.toString(),
                                    DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                                    DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 0
                            ))
                            .build(),
                    Document.builder()
                            .id(chunkId2)
                            .text("要刪除的文件 2")
                            .metadata(Map.of(
                                    DocumentChunkVectorStore.METADATA_VERSION_ID, testVersionId.toString(),
                                    DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                                    DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 1
                            ))
                            .build()
            ));

            // When - 刪除指定 ID
            vectorStore.delete(List.of(chunkId1));

            // Then - 驗證只刪除了指定的文件
            Integer remainingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE document_id = ?::uuid",
                    Integer.class,
                    documentId
            );
            assertThat(remainingCount).isEqualTo(1);

            // 確認正確的文件被保留
            String remainingId = jdbcTemplate.queryForObject(
                    "SELECT id::text FROM document_chunks WHERE document_id = ?::uuid",
                    String.class,
                    documentId
            );
            assertThat(remainingId).isEqualTo(chunkId2);
        }

        @Test
        @DisplayName("依 Filter Expression 刪除文件")
        void shouldDeleteByFilterExpression() {
            // Given - 建立兩個版本的測試資料
            UUID versionId2 = createTestVersion(testLibraryId, "2.0.0");

            UUID doc1 = createTestDocument(testVersionId, "/test/del/v1/doc1.md");
            UUID doc2 = createTestDocument(testVersionId, "/test/del/v1/doc2.md");
            UUID doc3 = createTestDocument(versionId2, "/test/del/v2/doc1.md");

            vectorStore.add(List.of(
                    createChunkDocument(testVersionId, doc1, 0, "版本 1 文件 1"),
                    createChunkDocument(testVersionId, doc2, 0, "版本 1 文件 2")
            ));
            vectorStore.add(List.of(
                    createChunkDocument(versionId2, doc3, 0, "版本 2 文件 1")
            ));

            // When - 刪除特定版本的所有文件
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            vectorStore.delete(filterBuilder.eq(
                    DocumentChunkVectorStore.METADATA_VERSION_ID,
                    testVersionId.toString()
            ).build());

            // Then - 只應刪除版本 1 的文件
            Integer remainingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks",
                    Integer.class
            );
            assertThat(remainingCount).isEqualTo(1);

            // 確認版本 2 的文件被保留
            String remainingVersionId = jdbcTemplate.queryForObject(
                    "SELECT metadata->>'versionId' FROM document_chunks",
                    String.class
            );
            assertThat(remainingVersionId).isEqualTo(versionId2.toString());
        }
    }

    // ==================== 輔助方法 ====================

    /**
     * 建立測試用的 Library 記錄
     */
    private UUID createTestLibrary() {
        UUID libraryId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO libraries (id, name, display_name, source_type, category)
            VALUES (?::uuid, ?, ?, ?, ?)
            """,
                libraryId.toString(),
                "test-lib-" + libraryId.toString().substring(0, 8),
                "Test Library",
                "GITHUB",
                "test"
        );
        return libraryId;
    }

    /**
     * 建立測試用的 LibraryVersion 記錄（使用預設版本號）
     */
    private UUID createTestVersion(UUID libraryId) {
        return createTestVersion(libraryId, "1.0.0");
    }

    /**
     * 建立測試用的 LibraryVersion 記錄（指定版本號）
     */
    private UUID createTestVersion(UUID libraryId, String version) {
        UUID versionId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO library_versions (id, library_id, version, is_latest, status)
            VALUES (?::uuid, ?::uuid, ?, ?, ?)
            """,
                versionId.toString(),
                libraryId.toString(),
                version,
                "1.0.0".equals(version),
                "ACTIVE"
        );
        return versionId;
    }

    /**
     * 建立測試用的 Document 記錄並返回其 ID
     */
    private UUID createTestDocument(UUID versionId, String path) {
        UUID documentId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO documents (id, version_id, title, path, content, doc_type)
            VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)
            """,
                documentId.toString(),
                versionId.toString(),
                "Test Document - " + path,
                path,
                "Test content for " + path,
                "MARKDOWN"
        );
        return documentId;
    }

    /**
     * 建立測試用的 Chunk Document（Spring AI Document 格式）
     */
    private Document createChunkDocument(UUID versionId, UUID documentId, int chunkIndex, String content) {
        return Document.builder()
                .id(UUID.randomUUID().toString())
                .text(content)
                .metadata(Map.of(
                        DocumentChunkVectorStore.METADATA_VERSION_ID, versionId.toString(),
                        DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId.toString(),
                        DocumentChunkVectorStore.METADATA_CHUNK_INDEX, chunkIndex,
                        DocumentChunkVectorStore.METADATA_TOKEN_COUNT, content.length()
                ))
                .build();
    }
}
