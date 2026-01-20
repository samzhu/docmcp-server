package io.github.samzhu.docmcp.infrastructure.vectorstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

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
 * 支援透過 metadata 中的 versionId 進行過濾。
 * </p>
 * <p>
 * 此實作的特點：
 * <ul>
 *   <li>使用 PostgreSQL pgvector 擴展進行向量相似度計算</li>
 *   <li>透過 JSONB metadata 欄位支援彈性的過濾條件</li>
 *   <li>支援批次 embedding 生成</li>
 *   <li>與 Spring AI 生態系統（如 RAG Advisor）相容</li>
 * </ul>
 * </p>
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

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final int dimensions;
    private final DocumentChunkFilterExpressionConverter filterConverter;

    /**
     * 建構子
     *
     * @param jdbcTemplate   JDBC 操作模板
     * @param embeddingModel 嵌入模型（用於生成向量）
     * @param dimensions     向量維度（預設 768）
     */
    public DocumentChunkVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingModel embeddingModel,
                                     int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.filterConverter = new DocumentChunkFilterExpressionConverter();

        log.info("初始化 DocumentChunkVectorStore，向量維度: {}", dimensions);
    }

    /**
     * 新增文件到向量儲存
     * <p>
     * 自動生成 embedding。使用 UPSERT 語法（ON CONFLICT DO UPDATE）處理重複 ID。
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

        // 批次生成 embedding
        List<String> texts = documents.stream()
                .map(Document::getText)
                .toList();
        List<float[]> embeddings = embeddingModel.embed(texts);

        // 使用批次更新插入
        String sql = """
            INSERT INTO document_chunks (id, document_id, chunk_index, content, embedding, token_count, metadata, created_at)
            VALUES (?::uuid, ?::uuid, ?, ?, ?::vector, ?, ?::jsonb, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                token_count = EXCLUDED.token_count,
                metadata = EXCLUDED.metadata
            """;

        jdbcTemplate.batchUpdate(sql, documents, 100, (ps, doc) -> {
            int index = documents.indexOf(doc);
            Map<String, Object> metadata = doc.getMetadata();

            // 取得或生成 ID
            String id = doc.getId() != null ? doc.getId() : UUID.randomUUID().toString();
            ps.setString(1, id);

            // 從 metadata 取得 documentId
            String documentId = getStringFromMetadata(metadata, METADATA_DOCUMENT_ID, UUID.randomUUID().toString());
            ps.setString(2, documentId);

            // 從 metadata 取得 chunkIndex
            int chunkIndex = getIntFromMetadata(metadata, METADATA_CHUNK_INDEX, 0);
            ps.setInt(3, chunkIndex);

            // 設定內容
            ps.setString(4, doc.getText());

            // 設定向量
            ps.setString(5, toVectorString(embeddings.get(index)));

            // 從 metadata 取得 tokenCount
            int tokenCount = getIntFromMetadata(metadata, METADATA_TOKEN_COUNT, 0);
            ps.setInt(6, tokenCount);

            // 設定 metadata（轉換為 JSON 字串）
            ps.setString(7, toJsonString(metadata));
        });

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

        String sql = "DELETE FROM document_chunks WHERE id = ANY(?::uuid[])";
        String[] ids = idList.toArray(new String[0]);
        jdbcTemplate.update(sql, (Object) ids);
    }

    /**
     * 依 Filter Expression 刪除文件
     * <p>
     * 將 Spring AI 的 Filter Expression 轉換為 JSONB 查詢條件進行刪除。
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

        String whereClause = filterConverter.convertExpression(filterExpression);
        if (whereClause == null || whereClause.isBlank()) {
            return;
        }

        log.info("依條件刪除文件區塊，條件: {}", whereClause);

        String sql = "DELETE FROM document_chunks WHERE " + whereClause;
        int deleted = jdbcTemplate.update(sql);

        log.debug("已刪除 {} 個文件區塊", deleted);
    }

    /**
     * 向量相似度搜尋
     * <p>
     * 使用 pgvector 的餘弦距離進行相似度搜尋。
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
        String queryVector = toVectorString(queryEmbedding);

        // 建構 SQL
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("""
            SELECT dc.id, dc.document_id, dc.chunk_index, dc.content,
                   dc.embedding, dc.token_count, dc.metadata,
                   1 - (dc.embedding <=> ?::vector) AS similarity
            FROM document_chunks dc
            WHERE dc.embedding IS NOT NULL
            """);

        // 處理過濾條件
        if (request.getFilterExpression() != null) {
            String whereClause = filterConverter.convertExpression(request.getFilterExpression());
            if (whereClause != null && !whereClause.isBlank()) {
                sqlBuilder.append(" AND ").append(whereClause);
            }
        }

        // 相似度閾值過濾
        double threshold = request.getSimilarityThreshold();
        if (threshold > 0) {
            sqlBuilder.append(" AND 1 - (dc.embedding <=> ?::vector) >= ").append(threshold);
        }

        // 排序和限制
        sqlBuilder.append(" ORDER BY similarity DESC LIMIT ?");

        String sql = sqlBuilder.toString();
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;

        // 執行查詢
        List<Document> results = jdbcTemplate.query(sql, new DocumentRowMapper(), queryVector, topK);

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
     * 將 float[] 轉換為 PostgreSQL vector 字串格式
     */
    private String toVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 將 Map 轉換為 JSON 字串
     */
    private String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                Object value = entry.getValue();
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof String) {
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    sb.append(value);
                } else {
                    sb.append("\"").append(escapeJson(value.toString())).append("\"");
                }
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.warn("JSON 轉換失敗，使用空物件", e);
            return "{}";
        }
    }

    /**
     * 轉義 JSON 特殊字元
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
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
     * </p>
     */
    private static class DocumentRowMapper implements RowMapper<Document> {
        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 解析 metadata JSONB
            Map<String, Object> metadata = parseMetadata(rs.getString("metadata"));

            // 加入相似度分數
            try {
                double similarity = rs.getDouble("similarity");
                metadata.put("score", similarity);
            } catch (SQLException ignored) {
                // 某些查詢可能沒有 similarity 欄位
            }

            // 建立 Document（Spring AI 2.0 的 Document 不再儲存 embedding）
            return new Document(rs.getString("id"), rs.getString("content"), metadata);
        }

        /**
         * 解析 JSONB 字串為 Map
         */
        private Map<String, Object> parseMetadata(String json) {
            if (json == null || json.isBlank() || "{}".equals(json)) {
                return new HashMap<>();
            }
            try {
                // 簡單的 JSON 解析（不依賴外部函式庫）
                Map<String, Object> result = new HashMap<>();
                String content = json.trim();
                if (content.startsWith("{") && content.endsWith("}")) {
                    content = content.substring(1, content.length() - 1);
                    if (!content.isBlank()) {
                        // 簡化解析：只處理基本的 key-value 對
                        String[] pairs = content.split(",(?=\\s*\")");
                        for (String pair : pairs) {
                            int colonIndex = pair.indexOf(':');
                            if (colonIndex > 0) {
                                String key = pair.substring(0, colonIndex).trim();
                                String value = pair.substring(colonIndex + 1).trim();
                                // 移除引號
                                if (key.startsWith("\"") && key.endsWith("\"")) {
                                    key = key.substring(1, key.length() - 1);
                                }
                                if (value.startsWith("\"") && value.endsWith("\"")) {
                                    value = value.substring(1, value.length() - 1);
                                    result.put(key, value);
                                } else if ("null".equals(value)) {
                                    result.put(key, null);
                                } else if ("true".equals(value) || "false".equals(value)) {
                                    result.put(key, Boolean.parseBoolean(value));
                                } else {
                                    try {
                                        if (value.contains(".")) {
                                            result.put(key, Double.parseDouble(value));
                                        } else {
                                            result.put(key, Long.parseLong(value));
                                        }
                                    } catch (NumberFormatException e) {
                                        result.put(key, value);
                                    }
                                }
                            }
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
    }

    /**
     * Filter Expression 轉換器
     * <p>
     * 將 Spring AI 的 Filter.Expression 轉換為 PostgreSQL JSONB 查詢條件。
     * </p>
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
                    yield "(" + left + " AND " + right + ")";
                }
                case OR -> {
                    String left = doConvert((Filter.Expression) expression.left());
                    String right = doConvert((Filter.Expression) expression.right());
                    yield "(" + left + " OR " + right + ")";
                }
                case NOT -> {
                    String operand = doConvert((Filter.Expression) expression.left());
                    yield "NOT (" + operand + ")";
                }
                case EQ -> convertComparison(expression, "=");
                case NE -> convertComparison(expression, "!=");
                case GT -> convertComparison(expression, ">");
                case GTE -> convertComparison(expression, ">=");
                case LT -> convertComparison(expression, "<");
                case LTE -> convertComparison(expression, "<=");
                case IN -> convertIn(expression);
                case NIN -> "NOT " + convertIn(expression);
                case ISNULL -> convertIsNull(expression, true);
                case ISNOTNULL -> convertIsNull(expression, false);
            };
        }

        private String convertIsNull(Filter.Expression expression, boolean isNull) {
            Filter.Key key = (Filter.Key) expression.left();
            String keyName = key.key();
            if (isNull) {
                return String.format("dc.metadata->>'%s' IS NULL", escapeForSql(keyName));
            } else {
                return String.format("dc.metadata->>'%s' IS NOT NULL", escapeForSql(keyName));
            }
        }

        private String convertComparison(Filter.Expression expression, String operator) {
            Filter.Key key = (Filter.Key) expression.left();
            Filter.Value value = (Filter.Value) expression.right();
            String keyName = key.key();
            Object val = value.value();

            // 使用 JSONB 運算子查詢
            if (val instanceof String) {
                return String.format("dc.metadata->>'%s' %s '%s'",
                        escapeForSql(keyName), operator, escapeForSql(val.toString()));
            } else if (val instanceof Number) {
                return String.format("(dc.metadata->>'%s')::numeric %s %s",
                        escapeForSql(keyName), operator, val);
            } else if (val instanceof Boolean) {
                return String.format("(dc.metadata->>'%s')::boolean %s %s",
                        escapeForSql(keyName), operator, val);
            }
            return String.format("dc.metadata->>'%s' %s '%s'",
                    escapeForSql(keyName), operator, escapeForSql(val.toString()));
        }

        private String convertIn(Filter.Expression expression) {
            Filter.Key key = (Filter.Key) expression.left();
            Filter.Value value = (Filter.Value) expression.right();
            String keyName = key.key();
            Object val = value.value();

            if (val instanceof List<?> list) {
                String values = list.stream()
                        .map(v -> "'" + escapeForSql(v.toString()) + "'")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                return String.format("dc.metadata->>'%s' IN (%s)",
                        escapeForSql(keyName), values);
            }
            return String.format("dc.metadata->>'%s' = '%s'",
                    escapeForSql(keyName), escapeForSql(val.toString()));
        }

        private String escapeForSql(String str) {
            if (str == null) return "";
            return str.replace("'", "''");
        }
    }
}
