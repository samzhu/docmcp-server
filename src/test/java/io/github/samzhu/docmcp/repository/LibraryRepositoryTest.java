package io.github.samzhu.docmcp.repository;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LibraryRepository 整合測試
 * <p>
 * 使用 Testcontainers 進行資料庫整合測試。
 * </p>
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, io.github.samzhu.docmcp.TestConfig.class})
@ActiveProfiles("test")
class LibraryRepositoryTest {

    @Autowired
    private LibraryRepository libraryRepository;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        libraryRepository.deleteAll();
    }

    @Test
    void shouldFindByName() {
        // 測試根據名稱查找函式庫
        var library = createAndSaveLibrary("react", "React", SourceType.GITHUB, "frontend");

        var found = libraryRepository.findByName("react");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("react");
    }

    @Test
    void shouldReturnEmptyWhenLibraryNotFoundByName() {
        // 測試當函式庫不存在時回傳空
        var found = libraryRepository.findByName("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByCategory() {
        // 測試根據分類查找函式庫
        createAndSaveLibrary("react", "React", SourceType.GITHUB, "frontend");
        createAndSaveLibrary("vue", "Vue.js", SourceType.GITHUB, "frontend");
        createAndSaveLibrary("spring-boot", "Spring Boot", SourceType.GITHUB, "backend");

        var frontendLibraries = libraryRepository.findByCategory("frontend");

        assertThat(frontendLibraries).hasSize(2);
        assertThat(frontendLibraries)
                .extracting(Library::getName)
                .containsExactlyInAnyOrder("react", "vue");
    }

    @Test
    void shouldSaveAndRetrieveLibrary() {
        // 測試儲存並取得函式庫
        var library = Library.create(
                randomId(),
                "spring-boot",
                "Spring Boot",
                "Java framework",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java", "framework")
        );

        var saved = libraryRepository.save(library);
        var retrieved = libraryRepository.findById(saved.getId());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("spring-boot");
    }

    /**
     * 建立並儲存測試用的函式庫
     */
    private Library createAndSaveLibrary(String name, String displayName,
                                          SourceType sourceType, String category) {
        var library = Library.create(randomId(), name, displayName, null, sourceType, null, category, null);
        return libraryRepository.save(library);
    }
}
