package io.github.samzhu.docmcp.config;

import com.github.f4b6a3.tsid.TsidFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TSID 配置類別
 * <p>
 * 配置 TsidFactory Bean，用於生成時間排序的唯一識別碼。
 * TSID (Time-Sorted Unique Identifier) 具有以下特性：
 * <ul>
 *   <li>13 字元 Crockford Base32 格式</li>
 *   <li>時間戳前綴，天然支援時間排序</li>
 *   <li>比 UUID 更短且更易讀</li>
 * </ul>
 * </p>
 */
@Configuration
public class TsidConfig {

    /**
     * 建立 TsidFactory Bean
     * <p>
     * 單機使用，採用預設配置即可。
     * </p>
     *
     * @return TsidFactory 實例
     */
    @Bean
    public TsidFactory tsidFactory() {
        return TsidFactory.builder().build();
    }
}
