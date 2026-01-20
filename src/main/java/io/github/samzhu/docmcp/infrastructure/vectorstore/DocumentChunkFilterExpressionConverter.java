package io.github.samzhu.docmcp.infrastructure.vectorstore;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;

import java.util.List;

/**
 * Filter Expression 轉換器
 * <p>
 * 將 Spring AI 的 Filter.Expression 轉換為 PostgreSQL JSONPath 格式。
 * 參考 Spring AI PgVectorFilterExpressionConverter 實作。
 * </p>
 *
 * @see <a href="https://github.com/spring-projects/spring-ai/blob/main/vector-stores/spring-ai-pgvector-store/src/main/java/org/springframework/ai/vectorstore/pgvector/PgVectorFilterExpressionConverter.java">PgVectorFilterExpressionConverter</a>
 */
public class DocumentChunkFilterExpressionConverter implements FilterExpressionConverter {

    @Override
    public String convertExpression(Filter.Expression expression) {
        if (expression == null) {
            return "";
        }
        return doConvert(expression);
    }

    /**
     * 遞迴轉換 Filter Expression 為 JSONPath 格式
     */
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
