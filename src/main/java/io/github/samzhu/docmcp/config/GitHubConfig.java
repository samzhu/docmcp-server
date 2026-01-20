package io.github.samzhu.docmcp.config;

import io.github.samzhu.docmcp.infrastructure.github.GitHubFetchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub 相關配置
 * <p>
 * 啟用 GitHub 內容取得的配置屬性。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(GitHubFetchProperties.class)
public class GitHubConfig {
}
