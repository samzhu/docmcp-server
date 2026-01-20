package io.github.samzhu.docmcp.infrastructure.vectorstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DocumentChunk VectorStore 實作
 * <p>
 * 實作 Spring AI VectorStore 介面，使用現有的 document_chunks 表進行向量儲存和搜尋。
 * 參考 Spring AI 官方 PgVectorStore 實作方式。
 * </p>
 * <p>
 * 此實作的特點：
 * <ul>
 *   <li>使用 PostgreSQL pgvector 擴展進行向量相似度計算</li>
 *   <li>使用 JdbcTemplate + PGvector 物件進行參數綁定</li>
 *   <li>透過 JSONPath 格式進行 metadata 過濾</li>
 *   <li>支援批次 embedding 生成</li>
 *   <li>與 Spring AI 生態系統（如 RAG Advisor）相容</li>
 * </ul>
 * </p>
 *
 * @see <a href="https://github.com/spring-projects/spring-ai/blob/main/vector-stores/spring-ai-pgvector-store/src/main/java/org/springframework/ai/vectorstore/pgvector/PgVectorStore.java">Spring AI PgVectorStore</a>
 */
public class DocumentChunkVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkVectorStore.class);

    // Metadata 鍵名常數 - 用於 document_chunks.metadata JSONB 欄位
    public static final String METADATA_VERSION_ID = "versionId";
    public static final String METADATA_DOCUMENT_ID = "documentId";
    public static final String METADATA_CHUNK_INDEX = "chunkIndex";
    public static final String METADATA_TOKEN_COUNT = "tokenCount";
    public static final String METADATA_DOCUMENT_TITLE = "documentTitle";
    public static final String METADATA_DOCUMENT_PATH = "documentPath";

    // SQL 語句常數 - 參考 Spring AI PgVectorStore
    private static final String SQL_INSERT = """
        INSERT INTO document_chunks (id, document_id, chunk_index, content, embedding, token_count, metadata, created_at)
        VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO UPDATE SET
            content = EXCLUDED.content,
            embedding = EXCLUDED.embedding,
            token_count = EXCLUDED.token_count,
            metadata = EXCLUDED.metadata
        """;

    private static final String SQL_DELETE_BY_IDS = "DELETE FROM document_chunks WHERE id = ANY(?::uuid[])";

    // 相似度搜尋 SQL - 使用餘弦距離 (<=>)，參考 Spring AI 的格式
    // 注意：distance = 1 - similarity，所以 distance < threshold 等同於 similarity > (1 - threshold)
    private static final String SQL_SIMILARITY_SEARCH = """
        SELECT dc.id, dc.document_id, dc.chunk_index, dc.content,
               dc.embedding, dc.token_count, dc.metadata,
               dc.embedding <=> ? AS distance
        FROM document_chunks dc
        WHERE dc.embedding IS NOT NULL
        %s
        AND dc.embedding <=> ? < ?
        ORDER BY distance
        LIMIT ?
        """;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final int dimensions;
    private final FilterExpressionConverter filterExpressionConverter;
    private final DocumentRowMapper documentRowMapper;

    /**
     * 建構子
     *
     * @param jdbcTemplate   JDBC 操作模板
     * @param embeddingModel 嵌入模型（用於生成向量）
     * @param objectMapper   JSON 序列化工具
     * @param dimensions     向量維度（預設 768）
     */
    public DocumentChunkVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingModel embeddingModel,
                                     ObjectMapper objectMapper,
                                     int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.dimensions = dimensions;
        this.filterExpressionConverter = new DocumentChunkFilterExpressionConverter();
        this.documentRowMapper = new DocumentRowMapper(objectMapper);

        log.info("初始化 DocumentChunkVectorStore，向量維度: {}", dimensions);
    }

    // Google GenAI embedding API 限制每批最多 100 個請求
    private static final int EMBEDDING_BATCH_SIZE = 100;

    /**
     * 新增文件到向量儲存
     * <p>
     * 自動生成 embedding。使用 UPSERT 語法（ON CONFLICT DO UPDATE）處理重複 ID。
     * 使用 JdbcTemplate.batchUpdate 進行批次插入。
     * 為避免 Google GenAI 的批次限制（最多 100 個），會自動分批處理。
     * </p>
     *
     * @param documents 要新增的 Spring AI Document 列表
     */
    @Override
    @Transactional
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.info("新增 {} 個文件到 VectorStore", documents.size());

        // 分批處理以避免 embedding API 的批次限制
        for (int batchStart = 0; batchStart < documents.size(); batchStart += EMBEDDING_BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + EMBEDDING_BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(batchStart, batchEnd);

            log.debug("處理批次 {}-{} / {}", batchStart + 1, batchEnd, documents.size());

            // 批次生成 embedding
            List<String> texts = batch.stream()
                    .map(Document::getText)
                    .toList();
            List<float[]> embeddings = embeddingModel.embed(texts);

            // 使用 JdbcTemplate.batchUpdate 進行批次插入
            jdbcTemplate.batchUpdate(SQL_INSERT, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Document doc = batch.get(i);
                    Map<String, Object> metadata = doc.getMetadata();
                    float[] embedding = embeddings.get(i);

                    // 設定參數
                    ps.setString(1, doc.getId() != null ? doc.getId() : UUID.randomUUID().toString());
                    ps.setString(2, getStringFromMetadata(metadata, METADATA_DOCUMENT_ID, UUID.randomUUID().toString()));
                    ps.setInt(3, getIntFromMetadata(metadata, METADATA_CHUNK_INDEX, 0));
                    ps.setString(4, doc.getText());
                    ps.setObject(5, new PGvector(embedding));
                    ps.setInt(6, getIntFromMetadata(metadata, METADATA_TOKEN_COUNT, 0));
                    ps.setString(7, toJsonString(metadata));
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        }

        log.debug("成功新增 {} 個文件", documents.size());
    }

    /**
     * 依 ID 列表刪除文件
     *
     * @param idList 要刪除的文件 ID 列表
     */
    @Override
    @Transactional
    public void delete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }

        log.info("刪除 {} 個文件區塊", idList.size());

        String[] ids = idList.toArray(new String[0]);
        jdbcTemplate.update(SQL_DELETE_BY_IDS, (Object) ids);
    }

    /**
     * 依 Filter Expression 刪除文件
     * <p>
     * 將 Spring AI 的 Filter Expression 轉換為 JSONPath 查詢條件進行刪除。
     * </p>
     *
     * @param filterExpression 過濾條件表達式
     */
    @Override
    @Transactional
    public void delete(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return;
        }

        String jsonPathFilter = filterExpressionConverter.convertExpression(filterExpression);
        if (!StringUtils.hasText(jsonPathFilter)) {
            return;
        }

        log.info("依條件刪除文件區塊，條件: {}", jsonPathFilter);

        String sql = "DELETE FROM document_chunks WHERE metadata::jsonb @@ '" + jsonPathFilter + "'::jsonpath";
        int deleted = jdbcTemplate.update(sql);

        log.debug("已刪除 {} 個文件區塊", deleted);
    }

    /**
     * 向量相似度搜尋
     * <p>
     * 使用 pgvector 的餘弦距離進行相似度搜尋。
     * 參考 Spring AI PgVectorStore 實作，使用 JdbcTemplate + PGvector 物件。
     * 支援透過 filterExpression 過濾特定 versionId 的文件。
     * </p>
     *
     * @param request 搜尋請求（包含查詢文字、topK、similarityThreshold、filterExpression）
     * @return 相似度最高的 Document 列表
     */
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return List.of();
        }

        log.debug("執行語意搜尋，查詢: {}, topK: {}, threshold: {}",
                request.getQuery(), request.getTopK(), request.getSimilarityThreshold());

        // 將查詢文字轉換為向量
        float[] queryEmbedding = embeddingModel.embed(request.getQuery());
        PGvector queryVector = new PGvector(queryEmbedding);

        // 處理過濾條件 - 使用 JSONPath 格式
        String jsonPathFilter = "";
        if (request.getFilterExpression() != null) {
            String nativeFilterExpression = filterExpressionConverter.convertExpression(request.getFilterExpression());
            if (StringUtils.hasText(nativeFilterExpression)) {
                jsonPathFilter = " AND metadata::jsonb @@ '" + nativeFilterExpression + "'::jsonpath ";
            }
        }

        // 計算距離閾值：distance = 1 - similarity
        double distanceThreshold = 1 - request.getSimilarityThreshold();
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;

        // 建構 SQL
        String sql = String.format(SQL_SIMILARITY_SEARCH, jsonPathFilter);

        // 執行查詢 - 參考 Spring AI，直接傳遞 PGvector 物件
        List<Document> results = jdbcTemplate.query(
                sql,
                documentRowMapper,
                queryVector,      // 用於計算 distance
                queryVector,      // 用於 WHERE 條件
                distanceThreshold,
                topK
        );

        log.debug("語意搜尋完成，找到 {} 個結果", results.size());
        return results;
    }

    /**
     * 取得 VectorStore 名稱
     */
    @Override
    public String getName() {
        return "DocumentChunkVectorStore";
    }

    // ========== 私有輔助方法 ==========

    /**
     * 將 Map 轉換為 JSON 字串
     */
    private String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失敗，使用空物件", e);
            return "{}";
        }
    }

    /**
     * 從 metadata 取得字串值
     */
    private String getStringFromMetadata(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 從 metadata 取得整數值
     */
    private int getIntFromMetadata(Map<String, Object> metadata, String key, int defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) {
            return defaultValue;
        }
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Document RowMapper
     * <p>
     * 將資料庫查詢結果轉換為 Spring AI Document 物件。
     * distance 轉換為 similarity score (1 - distance)。
     * </p>
     */
    private static class DocumentRowMapper implements RowMapper<Document> {
        private static final Logger log = LoggerFactory.getLogger(DocumentRowMapper.class);
        private final ObjectMapper objectMapper;

        public DocumentRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 解析 metadata JSONB
            Map<String, Object> metadata = parseMetadata(rs.getString("metadata"));

            // 將 distance 轉換為 similarity score
            try {
                double distance = rs.getDouble("distance");
                double similarity = 1 - distance;
                metadata.put("score", similarity);
            } catch (SQLException ignored) {
                // 某些查詢可能沒有 distance 欄位
            }

            // 建立 Document（Spring AI 2.0 的 Document 不再儲存 embedding）
            return new Document(rs.getString("id"), rs.getString("content"), metadata);
        }

        /**
         * 解析 JSONB 字串為 Map，使用 ObjectMapper
         */
        private Map<String, Object> parseMetadata(String json) {
            if (json == null || json.isBlank() || "{}".equals(json)) {
                return new HashMap<>();
            }
            try {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("JSON 解析失敗: {}", e.getMessage());
                return new HashMap<>();
            }
        }
    }

    /**
     * Filter Expression 轉換器
     * <p>
     * 將 Spring AI 的 Filter.Expression 轉換為 PostgreSQL JSONPath 格式。
     * 參考 Spring AI PgVectorFilterExpressionConverter 實作。
     * </p>
     *
     * @see <a href="https://github.com/spring-projects/spring-ai/blob/main/vector-stores/spring-ai-pgvector-store/src/main/java/org/springframework/ai/vectorstore/pgvector/PgVectorFilterExpressionConverter.java">PgVectorFilterExpressionConverter</a>
     */
    private static class DocumentChunkFilterExpressionConverter implements FilterExpressionConverter {

        @Override
        public String convertExpression(Filter.Expression expression) {
            if (expression == null) {
                return "";
            }
            return doConvert(expression);
        }

        private String doConvert(Filter.Expression expression) {
            return switch (expression.type()) {
                case AND -> {
                    String left = doConvert((Filter.Expression) expression.left());
                    String right = doConvert((Filter.Expression) expression.right());
                    yield left + " && " + right;
                }
                case OR -> {
                    String left = doConvert((Filter.Expression) expression.left());
                    String right = doConvert((Filter.Expression) expression.right());
                    yield left + " || " + right;
                }
                case NOT -> {
                    String operand = doConvert((Filter.Expression) expression.left());
                    yield "!(" + operand + ")";
                }
                case EQ -> convertComparison(expression, "==");
                case NE -> convertComparison(expression, "!=");
                case GT -> convertComparison(expression, ">");
                case GTE -> convertComparison(expression, ">=");
                case LT -> convertComparison(expression, "<");
                case LTE -> convertComparison(expression, "<=");
                case IN -> convertIn(expression);
                case NIN -> "!(" + convertIn(expression) + ")";
                case ISNULL -> convertIsNull(expression, true);
                case ISNOTNULL -> convertIsNull(expression, false);
            };
        }

        /**
         * 轉換比較運算 - 使用 JSONPath 格式 $.key == "value"
         */
        private String convertComparison(Filter.Expression expression, String operator) {
            Filter.Key key = (Filter.Key) expression.left();
            Filter.Value value = (Filter.Value) expression.right();
            String keyName = key.key();
            Object val = value.value();

            // JSONPath 格式：$.key == "value" 或 $.key == 123
            if (val instanceof String) {
                return String.format("$.%s %s \"%s\"", keyName, operator, escapeJsonPath(val.toString()));
            } else {
                return String.format("$.%s %s %s", keyName, operator, val);
            }
        }

        /**
         * 轉換 IN 運算 - 使用多個 OR 條件
         * 格式：($.key == "val1" || $.key == "val2")
         */
        private String convertIn(Filter.Expression expression) {
            Filter.Key key = (Filter.Key) expression.left();
            Filter.Value value = (Filter.Value) expression.right();
            String keyName = key.key();
            Object val = value.value();

            if (val instanceof List<?> list) {
                String conditions = list.stream()
                        .map(v -> {
                            if (v instanceof String) {
                                return String.format("$.%s == \"%s\"", keyName, escapeJsonPath(v.toString()));
                            } else {
                                return String.format("$.%s == %s", keyName, v);
                            }
                        })
                        .reduce((a, b) -> a + " || " + b)
                        .orElse("true");
                return "(" + conditions + ")";
            }

            // 單一值時視為 EQ
            if (val instanceof String) {
                return String.format("$.%s == \"%s\"", keyName, escapeJsonPath(val.toString()));
            } else {
                return String.format("$.%s == %s", keyName, val);
            }
        }

        /**
         * 轉換 IS NULL / IS NOT NULL
         */
        private String convertIsNull(Filter.Expression expression, boolean isNull) {
            Filter.Key key = (Filter.Key) expression.left();
            String keyName = key.key();
            // JSONPath 的 exists() 函數
            if (isNull) {
                return "!(exists($.\"" + keyName + "\"))";
            } else {
                return "exists($.\"" + keyName + "\")";
            }
        }

        /**
         * 轉義 JSONPath 字串中的特殊字元
         */
        private String escapeJsonPath(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"");
        }
    }
}
