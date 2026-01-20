package io.github.samzhu.docmcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排程配置
 * <p>
 * 啟用 Spring 的排程功能，允許使用 @Scheduled 註解。
 * </p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
