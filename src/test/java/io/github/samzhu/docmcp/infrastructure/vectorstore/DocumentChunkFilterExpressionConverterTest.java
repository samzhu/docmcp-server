package io.github.samzhu.docmcp.infrastructure.vectorstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunkFilterExpressionConverter 單元測試
 * <p>
 * 測試 Filter Expression 轉換為 PostgreSQL JSONPath 格式的功能。
 * 參考 Spring AI PgVectorFilterExpressionConverterTests 測試模式。
 * </p>
 */
@DisplayName("DocumentChunkFilterExpressionConverter")
class DocumentChunkFilterExpressionConverterTest {

    private DocumentChunkFilterExpressionConverter converter;
    private FilterExpressionBuilder builder;

    @BeforeEach
    void setUp() {
        converter = new DocumentChunkFilterExpressionConverter();
        builder = new FilterExpressionBuilder();
    }

    // ==================== 基本比較運算測試 ====================

    @Nested
    @DisplayName("基本比較運算")
    class ComparisonTests {

        @Test
        @DisplayName("EQ - 字串相等")
        void testEqWithString() {
            // Given - 建立 versionId == "abc123" 條件
            Filter.Expression expression = builder
                    .eq("versionId", "abc123")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.versionId == \"abc123\"");
        }

        @Test
        @DisplayName("EQ - 數值相等")
        void testEqWithNumber() {
            // Given - 建立 chunkIndex == 5 條件
            Filter.Expression expression = builder
                    .eq("chunkIndex", 5)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式（數值不加引號）
            assertThat(result).isEqualTo("$.chunkIndex == 5");
        }

        @Test
        @DisplayName("NE - 不等於")
        void testNe() {
            // Given - 建立 status != "deleted" 條件
            Filter.Expression expression = builder
                    .ne("status", "deleted")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.status != \"deleted\"");
        }

        @Test
        @DisplayName("GT - 大於")
        void testGt() {
            // Given - 建立 score > 0.8 條件
            Filter.Expression expression = builder
                    .gt("score", 0.8)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.score > 0.8");
        }

        @Test
        @DisplayName("GTE - 大於等於")
        void testGte() {
            // Given - 建立 tokenCount >= 100 條件
            Filter.Expression expression = builder
                    .gte("tokenCount", 100)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.tokenCount >= 100");
        }

        @Test
        @DisplayName("LT - 小於")
        void testLt() {
            // Given - 建立 chunkIndex < 10 條件
            Filter.Expression expression = builder
                    .lt("chunkIndex", 10)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.chunkIndex < 10");
        }

        @Test
        @DisplayName("LTE - 小於等於")
        void testLte() {
            // Given - 建立 priority <= 5 條件
            Filter.Expression expression = builder
                    .lte("priority", 5)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.priority <= 5");
        }
    }

    // ==================== 集合運算測試 ====================

    @Nested
    @DisplayName("集合運算")
    class CollectionTests {

        @Test
        @DisplayName("IN - 值在列表中（字串）")
        void testInWithStrings() {
            // Given - 建立 category IN ["frontend", "backend"] 條件
            Filter.Expression expression = builder
                    .in("category", "frontend", "backend")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式（OR 條件組合）
            assertThat(result).isEqualTo("($.category == \"frontend\" || $.category == \"backend\")");
        }

        @Test
        @DisplayName("IN - 值在列表中（數值）")
        void testInWithNumbers() {
            // Given - 建立 priority IN [1, 2, 3] 條件
            Filter.Expression expression = builder
                    .in("priority", 1, 2, 3)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("($.priority == 1 || $.priority == 2 || $.priority == 3)");
        }

        @Test
        @DisplayName("NIN - 值不在列表中")
        void testNin() {
            // Given - 建立 status NOT IN ["deleted", "archived"] 條件
            Filter.Expression expression = builder
                    .nin("status", "deleted", "archived")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式（NOT + IN）
            assertThat(result).isEqualTo("!(($.status == \"deleted\" || $.status == \"archived\"))");
        }
    }

    // ==================== 邏輯運算測試 ====================

    @Nested
    @DisplayName("邏輯運算")
    class LogicalTests {

        @Test
        @DisplayName("AND - 邏輯與")
        void testAnd() {
            // Given - 建立 versionId == "v1" AND chunkIndex >= 0 條件
            Filter.Expression expression = builder
                    .and(
                            builder.eq("versionId", "v1"),
                            builder.gte("chunkIndex", 0)
                    )
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.versionId == \"v1\" && $.chunkIndex >= 0");
        }

        @Test
        @DisplayName("OR - 邏輯或")
        void testOr() {
            // Given - 建立 category == "frontend" OR category == "backend" 條件
            Filter.Expression expression = builder
                    .or(
                            builder.eq("category", "frontend"),
                            builder.eq("category", "backend")
                    )
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.category == \"frontend\" || $.category == \"backend\"");
        }

        @Test
        @DisplayName("NOT - 邏輯非")
        void testNot() {
            // Given - 建立 NOT (status == "deleted") 條件
            Filter.Expression expression = builder
                    .not(builder.eq("status", "deleted"))
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("!($.status == \"deleted\")");
        }

        @Test
        @DisplayName("複合邏輯 - AND + OR 組合")
        void testComplexLogical() {
            // Given - 建立 (versionId == "v1" AND (priority > 5 OR status == "active")) 條件
            Filter.Expression expression = builder
                    .and(
                            builder.eq("versionId", "v1"),
                            builder.or(
                                    builder.gt("priority", 5),
                                    builder.eq("status", "active")
                            )
                    )
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.versionId == \"v1\" && $.priority > 5 || $.status == \"active\"");
        }
    }

    // ==================== NULL 運算測試 ====================

    @Nested
    @DisplayName("NULL 運算")
    class NullTests {

        @Test
        @DisplayName("IS NULL - 欄位為空")
        void testIsNull() {
            // Given - 建立 description IS NULL 條件
            Filter.Expression expression = builder
                    .isNull("description")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式（使用 exists 函數）
            assertThat(result).isEqualTo("!(exists($.\"description\"))");
        }

        @Test
        @DisplayName("IS NOT NULL - 欄位非空")
        void testIsNotNull() {
            // Given - 建立 title IS NOT NULL 條件
            Filter.Expression expression = builder
                    .isNotNull("title")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("exists($.\"title\")");
        }
    }

    // ==================== 邊界情況測試 ====================

    @Nested
    @DisplayName("邊界情況")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 表達式返回空字串")
        void testNullExpression() {
            // When - 傳入 null
            String result = converter.convertExpression(null);

            // Then - 返回空字串
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("字串值包含特殊字元時正確轉義")
        void testEscapeSpecialCharacters() {
            // Given - 建立包含雙引號的條件
            Filter.Expression expression = builder
                    .eq("title", "Hello \"World\"")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證特殊字元被正確轉義
            assertThat(result).isEqualTo("$.title == \"Hello \\\"World\\\"\"");
        }

        @Test
        @DisplayName("字串值包含反斜線時正確轉義")
        void testEscapeBackslash() {
            // Given - 建立包含反斜線的條件
            Filter.Expression expression = builder
                    .eq("path", "C:\\Users\\test")
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證反斜線被正確轉義
            assertThat(result).isEqualTo("$.path == \"C:\\\\Users\\\\test\"");
        }

        @Test
        @DisplayName("使用 versionId 進行過濾（實際使用情境）")
        void testVersionIdFilter() {
            // Given - 實際的 versionId 過濾（UUID 格式）
            String versionId = "550e8400-e29b-41d4-a716-446655440000";
            Filter.Expression expression = builder
                    .eq(DocumentChunkVectorStore.METADATA_VERSION_ID, versionId)
                    .build();

            // When - 轉換
            String result = converter.convertExpression(expression);

            // Then - 驗證 JSONPath 格式
            assertThat(result).isEqualTo("$.versionId == \"550e8400-e29b-41d4-a716-446655440000\"");
        }
    }

    // ==================== 參數化測試 ====================

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @CsvSource({
            "category, ==, frontend, $.category == \"frontend\"",
            "count, >, 10, $.count > 10",
            "score, >=, 0.5, $.score >= 0.5",
            "index, <, 100, $.index < 100",
            "priority, <=, 3, $.priority <= 3",
            "status, !=, deleted, $.status != \"deleted\""
    })
    @DisplayName("參數化比較運算測試")
    void testParameterizedComparison(String key, String operator, String value, String expected) {
        // Given - 根據運算符建立表達式
        Filter.Expression expression = switch (operator) {
            case "==" -> builder.eq(key, parseValue(value)).build();
            case "!=" -> builder.ne(key, parseValue(value)).build();
            case ">" -> builder.gt(key, parseValue(value)).build();
            case ">=" -> builder.gte(key, parseValue(value)).build();
            case "<" -> builder.lt(key, parseValue(value)).build();
            case "<=" -> builder.lte(key, parseValue(value)).build();
            default -> throw new IllegalArgumentException("未知運算符: " + operator);
        };

        // When - 轉換
        String result = converter.convertExpression(expression);

        // Then - 驗證結果
        assertThat(result).isEqualTo(expected);
    }

    /**
     * 解析參數值（嘗試轉為數值，否則保持字串）
     */
    private Object parseValue(String value) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
