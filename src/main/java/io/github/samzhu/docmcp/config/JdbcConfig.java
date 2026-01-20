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
                new TimestampToOffsetDateTimeConverter()
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

}
