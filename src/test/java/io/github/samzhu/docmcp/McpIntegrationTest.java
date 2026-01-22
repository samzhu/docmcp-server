package io.github.samzhu.docmcp;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.ListLibrariesResult;
import io.github.samzhu.docmcp.mcp.tool.discovery.ListLibrariesTool;
import io.github.samzhu.docmcp.mcp.tool.discovery.ResolveLibraryTool;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MCP 整合測試
 * <p>
 * 測試 MCP 工具層是否正確運作。
 * </p>
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
@Tag("integration")
class McpIntegrationTest {

    @Autowired
    private ListLibrariesTool listLibrariesTool;

    @Autowired
    private ResolveLibraryTool resolveLibraryTool;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryVersionRepository libraryVersionRepository;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        // 清理測試資料
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        // 驗證應用程式上下文載入成功
        assertThat(listLibrariesTool).isNotNull();
        assertThat(resolveLibraryTool).isNotNull();
    }

    @Test
    void shouldListLibrariesWhenEmpty() {
        // 測試當沒有函式庫時，list_libraries 應回傳空列表
        ListLibrariesResult result = listLibrariesTool.listLibraries(null);

        assertThat(result.libraries()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    @Test
    void shouldListLibrariesWithData() {
        // 準備測試資料
        var library = Library.create(
                randomId(),
                "spring-boot",
                "Spring Boot",
                "Java framework for building applications",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                null
        );
        libraryRepository.save(library);

        // 測試 list_libraries
        var result = listLibrariesTool.listLibraries(null);

        assertThat(result.libraries()).hasSize(1);
        assertThat(result.libraries().getFirst().name()).isEqualTo("spring-boot");
    }

    @Test
    void shouldResolveLibrary() {
        // 準備測試資料
        var library = Library.create(
                randomId(),
                "react",
                "React",
                "A JavaScript library for building user interfaces",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                null
        );
        var savedLibrary = libraryRepository.save(library);

        var version = LibraryVersion.create(randomId(), savedLibrary.getId(), "18.2.0", true);
        libraryVersionRepository.save(version);

        // 測試 resolve_library
        var result = resolveLibraryTool.resolveLibrary("react", null);

        assertThat(result.name()).isEqualTo("react");
        assertThat(result.resolvedVersion()).isEqualTo("18.2.0");
    }

    @Test
    void shouldListLibrariesByCategory() {
        // 準備測試資料
        libraryRepository.save(Library.create(
                randomId(), "react", "React", null, SourceType.GITHUB, null, "frontend", null
        ));
        libraryRepository.save(Library.create(
                randomId(), "vue", "Vue.js", null, SourceType.GITHUB, null, "frontend", null
        ));
        libraryRepository.save(Library.create(
                randomId(), "spring-boot", "Spring Boot", null, SourceType.GITHUB, null, "backend", null
        ));

        // 測試根據分類篩選
        var result = listLibrariesTool.listLibraries("frontend");

        assertThat(result.libraries()).hasSize(2);
        assertThat(result.libraries())
                .allMatch(lib -> "frontend".equals(lib.category()));
    }
}
