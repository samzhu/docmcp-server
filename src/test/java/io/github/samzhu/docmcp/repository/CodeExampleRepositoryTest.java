package io.github.samzhu.docmcp.repository;

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
import java.util.UUID;

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

    private UUID libraryId;
    private UUID versionId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        // 清空資料
        codeExampleRepository.deleteAll();
        documentRepository.deleteAll();
        versionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試資料
        var library = Library.create("test-lib", "Test Library", "A test library",
                SourceType.GITHUB, "https://github.com/test/test-lib", "backend", List.of("java", "spring"));
        library = libraryRepository.save(library);
        libraryId = library.id();

        var version = LibraryVersion.create(libraryId, "1.0.0", true);
        version = versionRepository.save(version);
        versionId = version.id();

        var document = Document.create(versionId, "Test Doc", "/docs/test.md",
                "Test content", "abc123", "markdown");
        document = documentRepository.save(document);
        documentId = document.id();
    }

    @Test
    @DisplayName("should find code examples by document ID")
    void shouldFindCodeExamplesByDocumentId() {
        // Arrange
        var example1 = CodeExample.create(documentId, "java", "System.out.println(\"Hello\");", "Hello World");
        var example2 = CodeExample.create(documentId, "java", "int x = 42;", "Variable declaration");
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
        var results = codeExampleRepository.findByDocumentId(UUID.randomUUID());

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should find code examples by library and language")
    void shouldFindCodeExamplesByLibraryAndLanguage() {
        // Arrange
        var javaExample = CodeExample.create(documentId, "java", "public class Test {}", "Java class");
        var jsExample = CodeExample.create(documentId, "javascript", "console.log('Hello')", "JS log");
        codeExampleRepository.save(javaExample);
        codeExampleRepository.save(jsExample);

        // Act
        var results = codeExampleRepository.findByLibraryAndLanguage(libraryId, "1.0.0", "java", 10);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().language()).isEqualTo("java");
    }

    @Test
    @DisplayName("should find all code examples when language is null")
    void shouldFindAllCodeExamplesWhenLanguageIsNull() {
        // Arrange
        var javaExample = CodeExample.create(documentId, "java", "public class Test {}", "Java class");
        var jsExample = CodeExample.create(documentId, "javascript", "console.log('Hello')", "JS log");
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
            var example = CodeExample.create(documentId, "java", "code " + i, "Example " + i);
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
        var java1 = CodeExample.create(documentId, "java", "code1", "Example 1");
        var java2 = CodeExample.create(documentId, "java", "code2", "Example 2");
        var js = CodeExample.create(documentId, "javascript", "code3", "Example 3");
        var python = CodeExample.create(documentId, "python", "code4", "Example 4");
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
