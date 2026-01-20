package io.github.samzhu.docmcp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * JDBC 配置
 * <p>
 * 配置自訂的型別轉換器，支援 Map 和 JSONB 之間的轉換。
 * </p>
 */
@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(
                new MapToJsonbConverter(),
                new JsonbToMapConverter(),
                new TimestampToOffsetDateTimeConverter(),
                new VectorToFloatArrayConverter(),
                new FloatArrayToVectorConverter()
        ));
    }

    /**
     * Map -> JSONB 寫入轉換器
     */
    @WritingConverter
    public static class MapToJsonbConverter implements Converter<Map<String, Object>, PGobject> {
        @Override
        public PGobject convert(Map<String, Object> source) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            try {
                jsonObject.setValue(source == null ? "{}" : OBJECT_MAPPER.writeValueAsString(source));
            } catch (SQLException | JsonProcessingException e) {
                throw new RuntimeException("Error converting Map to JSONB", e);
            }
            return jsonObject;
        }
    }

    /**
     * JSONB -> Map 讀取轉換器
     */
    @ReadingConverter
    public static class JsonbToMapConverter implements Converter<PGobject, Map<String, Object>> {
        @Override
        public Map<String, Object> convert(PGobject source) {
            if (source == null || source.getValue() == null) {
                return Map.of();
            }
            try {
                return OBJECT_MAPPER.readValue(source.getValue(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting JSONB to Map", e);
            }
        }
    }

    /**
     * Timestamp -> OffsetDateTime 讀取轉換器
     */
    @ReadingConverter
    public static class TimestampToOffsetDateTimeConverter implements Converter<Timestamp, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Timestamp source) {
            if (source == null) {
                return null;
            }
            return source.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    /**
     * PGobject (vector) -> float[] 讀取轉換器
     * <p>
     * 將 PostgreSQL pgvector 的 vector 類型轉換為 Java float[]。
     * vector 格式: "[0.1,0.2,0.3,...]"
     * </p>
     */
    @ReadingConverter
    public static class VectorToFloatArrayConverter implements Converter<PGobject, float[]> {
        @Override
        public float[] convert(PGobject source) {
            if (source == null || source.getValue() == null) {
                return null;
            }
            String value = source.getValue();
            // 移除方括號 "[" 和 "]"
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }
            if (value.isEmpty()) {
                return new float[0];
            }
            String[] parts = value.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        }
    }

    /**
     * float[] -> PGobject (vector) 寫入轉換器
     * <p>
     * 將 Java float[] 轉換為 PostgreSQL pgvector 的 vector 類型。
     * </p>
     */
    @WritingConverter
    public static class FloatArrayToVectorConverter implements Converter<float[], PGobject> {
        @Override
        public PGobject convert(float[] source) {
            PGobject vectorObject = new PGobject();
            vectorObject.setType("vector");
            try {
                if (source == null || source.length == 0) {
                    vectorObject.setValue(null);
                } else {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < source.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(source[i]);
                    }
                    sb.append("]");
                    vectorObject.setValue(sb.toString());
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error converting float[] to vector", e);
            }
            return vectorObject;
        }
    }

}
