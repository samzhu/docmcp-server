package io.github.samzhu.docmcp.repository;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.CodeExample;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CodeExampleRepository 整合測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
@DisplayName("CodeExampleRepository")
class CodeExampleRepositoryTest {

    @Autowired
    private CodeExampleRepository codeExampleRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LibraryVersionRepository versionRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    private String libraryId;
    private String versionId;
    private String documentId;

    /**
     * 生成隨機 TSID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        // 清空資料
        codeExampleRepository.deleteAll();
        documentRepository.deleteAll();
        versionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試資料
        var library = Library.create(randomId(), "test-lib", "Test Library", "A test library",
                SourceType.GITHUB, "https://github.com/test/test-lib", "backend", List.of("java", "spring"));
        library = libraryRepository.save(library);
        libraryId = library.getId();

        var version = LibraryVersion.create(randomId(), libraryId, "1.0.0", true);
        version = versionRepository.save(version);
        versionId = version.getId();

        var document = Document.create(randomId(), versionId, "Test Doc", "/docs/test.md",
                "Test content", "abc123", "markdown");
        document = documentRepository.save(document);
        documentId = document.getId();
    }

    @Test
    @DisplayName("should find code examples by document ID")
    void shouldFindCodeExamplesByDocumentId() {
        // Arrange
        var example1 = CodeExample.create(randomId(), documentId, "java", "System.out.println(\"Hello\");", "Hello World");
        var example2 = CodeExample.create(randomId(), documentId, "java", "int x = 42;", "Variable declaration");
        codeExampleRepository.save(example1);
        codeExampleRepository.save(example2);

        // Act
        var results = codeExampleRepository.findByDocumentId(documentId);

        // Assert
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when no examples for document")
    void shouldReturnEmptyListWhenNoExamplesForDocument() {
        // Act
        var results = codeExampleRepository.findByDocumentId(randomId());

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should find code examples by library and language")
    void shouldFindCodeExamplesByLibraryAndLanguage() {
        // Arrange
        var javaExample = CodeExample.create(randomId(), documentId, "java", "public class Test {}", "Java class");
        var jsExample = CodeExample.create(randomId(), documentId, "javascript", "console.log('Hello')", "JS log");
        codeExampleRepository.save(javaExample);
        codeExampleRepository.save(jsExample);

        // Act
        var results = codeExampleRepository.findByLibraryAndLanguage(libraryId, "1.0.0", "java", 10);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getLanguage()).isEqualTo("java");
    }

    @Test
    @DisplayName("should find all code examples when language is null")
    void shouldFindAllCodeExamplesWhenLanguageIsNull() {
        // Arrange
        var javaExample = CodeExample.create(randomId(), documentId, "java", "public class Test {}", "Java class");
        var jsExample = CodeExample.create(randomId(), documentId, "javascript", "console.log('Hello')", "JS log");
        codeExampleRepository.save(javaExample);
        codeExampleRepository.save(jsExample);

        // Act
        var results = codeExampleRepository.findByLibraryAndLanguage(libraryId, "1.0.0", null, 10);

        // Assert
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("should respect limit")
    void shouldRespectLimit() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            var example = CodeExample.create(randomId(), documentId, "java", "code " + i, "Example " + i);
            codeExampleRepository.save(example);
        }

        // Act
        var results = codeExampleRepository.findByLibraryAndLanguage(libraryId, "1.0.0", null, 3);

        // Assert
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("should find distinct languages by library")
    void shouldFindDistinctLanguagesByLibrary() {
        // Arrange
        var java1 = CodeExample.create(randomId(), documentId, "java", "code1", "Example 1");
        var java2 = CodeExample.create(randomId(), documentId, "java", "code2", "Example 2");
        var js = CodeExample.create(randomId(), documentId, "javascript", "code3", "Example 3");
        var python = CodeExample.create(randomId(), documentId, "python", "code4", "Example 4");
        codeExampleRepository.save(java1);
        codeExampleRepository.save(java2);
        codeExampleRepository.save(js);
        codeExampleRepository.save(python);

        // Act
        var languages = codeExampleRepository.findDistinctLanguagesByLibrary(libraryId, null);

        // Assert
        assertThat(languages).hasSize(3);
        assertThat(languages).containsExactlyInAnyOrder("java", "javascript", "python");
    }
}
