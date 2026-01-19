package io.github.samzhu.docmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 應用程式基本測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class DocMcpServerApplicationTests {

	@Test
	void contextLoads() {
		// 驗證應用程式上下文載入成功
	}

}
