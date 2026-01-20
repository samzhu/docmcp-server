package io.github.samzhu.docmcp.infrastructure.vectorstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DocumentChunkVectorStore 單元測試
 * <p>
 * 測試 VectorStore 核心功能：新增、搜尋、刪除文件區塊。
 * 使用 Mockito 模擬 JdbcTemplate、EmbeddingModel、ObjectMapper 依賴。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentChunkVectorStore")
class DocumentChunkVectorStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ObjectMapper objectMapper;

    private DocumentChunkVectorStore vectorStore;

    // 測試用向量維度
    private static final int DIMENSIONS = 768;

    @BeforeEach
    void setUp() {
        vectorStore = new DocumentChunkVectorStore(
                jdbcTemplate,
                embeddingModel,
                objectMapper,
                DIMENSIONS
        );
    }

    // ==================== add() 方法測試 ====================

    @Nested
    @DisplayName("add() 方法")
    class AddTests {

        @Test
        @DisplayName("正常新增文件並生成 embedding")
        void shouldInsertDocuments_whenValidInput() throws Exception {
            // Given - 準備測試文件
            String docId = UUID.randomUUID().toString();
            String documentId = UUID.randomUUID().toString();
            Document document = Document.builder()
                    .id(docId)
                    .text("這是測試內容")
                    .metadata(Map.of(
                            DocumentChunkVectorStore.METADATA_VERSION_ID, UUID.randomUUID().toString(),
                            DocumentChunkVectorStore.METADATA_DOCUMENT_ID, documentId,
                            DocumentChunkVectorStore.METADATA_CHUNK_INDEX, 0,
                            DocumentChunkVectorStore.METADATA_TOKEN_COUNT, 10
                    ))
                    .build();
            List<Document> documents = List.of(document);

            // Mock embedding 生成
            float[] embedding = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                embedding[i] = 0.1f;
            }
            when(embeddingModel.embed(List.of("這是測試內容"))).thenReturn(List.of(embedding));

            // Mock JSON 序列化
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");

            // When - 執行新增操作
            vectorStore.add(documents);

            // Then - 驗證 batchUpdate 被呼叫
            verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
            verify(embeddingModel, times(1)).embed(List.of("這是測試內容"));
        }

        @Test
        @DisplayName("傳入空列表時不執行任何操作")
        void shouldDoNothing_whenEmptyList() {
            // When - 傳入空列表
            vectorStore.add(List.of());

            // Then - 不應有任何資料庫或 embedding 操作
            verifyNoInteractions(jdbcTemplate);
            verifyNoInteractions(embeddingModel);
        }

        @Test
        @DisplayName("傳入 null 時不拋出例外")
        void shouldDoNothing_whenNullInput() {
            // When - 傳入 null
            vectorStore.add(null);

            // Then - 不應有任何資料庫或 embedding 操作
            verifyNoInteractions(jdbcTemplate);
            verifyNoInteractions(embeddingModel);
        }
    }

    // ==================== similaritySearch() 方法測試 ====================

    @Nested
    @DisplayName("similaritySearch() 方法")
    class SimilaritySearchTests {

        @Test
        @DisplayName("正常搜尋並返回結果")
        void shouldReturnResults_whenValidQuery() {
            // Given - 準備搜尋請求
            String query = "Spring Boot 設定";
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .similarityThreshold(0.7)
                    .build();

            // Mock embedding 生成
            float[] queryEmbedding = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                queryEmbedding[i] = 0.2f;
            }
            when(embeddingModel.embed(query)).thenReturn(queryEmbedding);

            // Mock 資料庫查詢結果（返回空列表，因為我們只測試流程）
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When - 執行搜尋
            List<Document> results = vectorStore.similaritySearch(request);

            // Then - 驗證 embedding 生成和資料庫查詢被呼叫
            verify(embeddingModel, times(1)).embed(query);
            verify(jdbcTemplate, times(1)).query(anyString(), any(RowMapper.class), any(), any(), any(), any());
            assertThat(results).isNotNull();
        }

        @Test
        @DisplayName("空查詢時返回空列表")
        void shouldReturnEmptyList_whenQueryIsBlank() {
            // Given - 空查詢
            SearchRequest requestWithBlank = SearchRequest.builder()
                    .query("   ")
                    .topK(5)
                    .build();

            // When - 執行搜尋
            List<Document> results = vectorStore.similaritySearch(requestWithBlank);

            // Then - 不應有任何 embedding 或資料庫操作
            verifyNoInteractions(embeddingModel);
            verifyNoInteractions(jdbcTemplate);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("空字串查詢時返回空列表")
        void shouldReturnEmptyList_whenQueryIsEmptyString() {
            // Given - 空字串查詢
            SearchRequest requestWithEmpty = SearchRequest.builder()
                    .query("")
                    .topK(5)
                    .build();

            // When - 執行搜尋
            List<Document> results = vectorStore.similaritySearch(requestWithEmpty);

            // Then - 不應有任何 embedding 或資料庫操作
            verifyNoInteractions(embeddingModel);
            verifyNoInteractions(jdbcTemplate);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("使用 filter 過濾 versionId")
        void shouldApplyFilter_whenFilterExpressionProvided() {
            // Given - 準備帶有 filter 的搜尋請求
            String query = "API 文件";
            String versionId = UUID.randomUUID().toString();

            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            Filter.Expression filterExpression = filterBuilder.eq(
                    DocumentChunkVectorStore.METADATA_VERSION_ID, versionId
            ).build();

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(10)
                    .similarityThreshold(0.5)
                    .filterExpression(filterExpression)
                    .build();

            // Mock embedding 生成
            float[] queryEmbedding = new float[DIMENSIONS];
            when(embeddingModel.embed(query)).thenReturn(queryEmbedding);

            // Mock 資料庫查詢結果
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When - 執行搜尋
            List<Document> results = vectorStore.similaritySearch(request);

            // Then - 驗證查詢包含 filter 條件
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, times(1)).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any());

            // SQL 應該包含 JSONPath 過濾條件
            String executedSql = sqlCaptor.getValue();
            assertThat(executedSql).contains("metadata::jsonb @@");
            assertThat(results).isNotNull();
        }
    }

    // ==================== delete(List<String>) 方法測試 ====================

    @Nested
    @DisplayName("delete(List<String>) 方法")
    class DeleteByIdListTests {

        @Test
        @DisplayName("正常刪除指定 ID")
        void shouldDeleteDocuments_whenValidIdList() {
            // Given - 準備要刪除的 ID 列表
            List<String> idList = List.of(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
            );

            // When - 執行刪除
            vectorStore.delete(idList);

            // Then - 驗證 batchUpdate 被呼叫
            verify(jdbcTemplate, times(1)).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        }

        @Test
        @DisplayName("空列表時不執行操作")
        void shouldDoNothing_whenIdListIsEmpty() {
            // When - 傳入空列表
            vectorStore.delete(List.of());

            // Then - 不應有任何資料庫操作
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("null 列表時不執行操作")
        void shouldDoNothing_whenIdListIsNull() {
            // When - 傳入 null
            vectorStore.delete((List<String>) null);

            // Then - 不應有任何資料庫操作
            verifyNoInteractions(jdbcTemplate);
        }
    }

    // ==================== delete(Filter.Expression) 方法測試 ====================

    @Nested
    @DisplayName("delete(Filter.Expression) 方法")
    class DeleteByFilterTests {

        @Test
        @DisplayName("依條件刪除文件")
        void shouldDeleteDocuments_whenValidFilterExpression() {
            // Given - 準備過濾條件
            String versionId = UUID.randomUUID().toString();
            FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
            Filter.Expression filterExpression = filterBuilder.eq(
                    DocumentChunkVectorStore.METADATA_VERSION_ID, versionId
            ).build();

            // Mock 刪除操作返回結果
            when(jdbcTemplate.update(anyString())).thenReturn(5);

            // When - 執行刪除
            vectorStore.delete(filterExpression);

            // Then - 驗證 update 被呼叫
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate, times(1)).update(sqlCaptor.capture());

            // SQL 應該包含 JSONPath 過濾條件
            String executedSql = sqlCaptor.getValue();
            assertThat(executedSql).contains("DELETE FROM document_chunks");
            assertThat(executedSql).contains("metadata::jsonb @@");
        }

        @Test
        @DisplayName("null 條件時不執行操作")
        void shouldDoNothing_whenFilterExpressionIsNull() {
            // When - 傳入 null
            vectorStore.delete((Filter.Expression) null);

            // Then - 不應有任何資料庫操作
            verify(jdbcTemplate, never()).update(anyString());
        }
    }

    // ==================== getName() 方法測試 ====================

    @Test
    @DisplayName("getName() 應返回正確的名稱")
    void getName_shouldReturnCorrectName() {
        // When
        String name = vectorStore.getName();

        // Then
        assertThat(name).isEqualTo("DocumentChunkVectorStore");
    }
}
