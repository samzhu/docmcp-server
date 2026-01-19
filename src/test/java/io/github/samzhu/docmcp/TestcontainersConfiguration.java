package io.github.samzhu.docmcp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 配置
 * <p>
 * 提供測試環境所需的 PostgreSQL + pgvector 容器。
 * 使用 {@link ServiceConnection} 自動配置 DataSource 連線。
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * 建立 PostgreSQL + pgvector 容器
	 * <p>
	 * 使用 pgvector/pgvector:pg16 映像，支援向量儲存功能。
	 * </p>
	 *
	 * @return PostgreSQL 容器實例
	 */
	@Bean
	@ServiceConnection
	@SuppressWarnings("resource")
	PostgreSQLContainer<?> pgvectorContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
				.asCompatibleSubstituteFor("postgres"));
	}

}
