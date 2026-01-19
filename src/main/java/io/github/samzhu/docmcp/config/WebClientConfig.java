package io.github.samzhu.docmcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Web Client 配置
 * <p>
 * 提供 RestClient.Builder 和 ObjectMapper Bean 供其他元件使用。
 * </p>
 */
@Configuration
public class WebClientConfig {

    /**
     * 建立 RestClient.Builder
     *
     * @return RestClient.Builder 實例
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 建立 ObjectMapper（如果不存在）
     *
     * @return ObjectMapper 實例
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
