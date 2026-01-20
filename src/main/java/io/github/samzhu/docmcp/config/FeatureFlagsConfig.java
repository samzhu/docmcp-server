package io.github.samzhu.docmcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 功能開關配置啟用
 * <p>
 * 啟用 FeatureFlags 配置屬性綁定。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(FeatureFlags.class)
public class FeatureFlagsConfig {
}
