package io.github.samzhu.docmcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 執行緒池配置
 * <p>
 * 使用 Java 21+ 的 Virtual Threads 提升並發效能。
 * Virtual Threads 適合處理 I/O 密集型任務，如資料庫查詢和網路請求。
 * </p>
 */
@Configuration
public class ExecutorConfig {

    /**
     * 建立使用 Virtual Threads 的執行緒池
     * <p>
     * Virtual Threads 是輕量級執行緒，可以大幅提升高並發場景的效能。
     * </p>
     *
     * @return 執行緒池服務
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
