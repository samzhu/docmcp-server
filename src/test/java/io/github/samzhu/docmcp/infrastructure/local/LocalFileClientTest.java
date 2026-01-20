package io.github.samzhu.docmcp.infrastructure.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileClient 單元測試
 */
class LocalFileClientTest {

    private LocalFileClient localFileClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        localFileClient = new LocalFileClient();
    }

    @Test
    @DisplayName("應讀取符合模式的所有文件")
    void shouldReadFilesMatchingPattern() throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("file1.md"), "# File 1");
        Files.writeString(tempDir.resolve("file2.md"), "# File 2");
        Files.writeString(tempDir.resolve("file3.txt"), "Not markdown");

        // Act
        var files = localFileClient.readDirectory(tempDir, "*.md");

        // Assert
        assertThat(files).hasSize(2);
        assertThat(files).extracting(LocalFileClient.FileContent::path)
                .containsExactlyInAnyOrder("file1.md", "file2.md");
    }

    @Test
    @DisplayName("應遞迴讀取子目錄中的文件")
    void shouldReadFilesRecursively() throws IOException {
        // Arrange
        Files.createDirectories(tempDir.resolve("docs/api"));
        Files.writeString(tempDir.resolve("docs/intro.md"), "# Intro");
        Files.writeString(tempDir.resolve("docs/api/reference.md"), "# API Reference");

        // Act - **/*.md 只匹配子目錄中的文件
        var files = localFileClient.readDirectory(tempDir, "**/*.md");

        // Assert
        assertThat(files).hasSize(2);
        assertThat(files).extracting(LocalFileClient.FileContent::path)
                .containsExactlyInAnyOrder("docs/intro.md", "docs/api/reference.md");
    }

    @Test
    @DisplayName("應回傳文件內容")
    void shouldReturnFileContent() throws IOException {
        // Arrange
        String content = "# Getting Started\n\nWelcome to the docs!";
        Files.writeString(tempDir.resolve("getting-started.md"), content);

        // Act
        var files = localFileClient.readDirectory(tempDir, "*.md");

        // Assert
        assertThat(files).hasSize(1);
        assertThat(files.get(0).content()).isEqualTo(content);
    }

    @Test
    @DisplayName("應讀取單一文件")
    void shouldReadSingleFile() throws IOException {
        // Arrange
        String content = "# Single File";
        Path filePath = tempDir.resolve("single.md");
        Files.writeString(filePath, content);

        // Act
        var file = localFileClient.readFile(filePath);

        // Assert
        assertThat(file.path()).isEqualTo("single.md");
        assertThat(file.content()).isEqualTo(content);
        assertThat(file.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("當目錄不存在時應拋出例外")
    void shouldThrowExceptionWhenDirectoryNotExists() {
        // Arrange
        Path nonExistentDir = tempDir.resolve("non-existent");

        // Act & Assert
        assertThatThrownBy(() -> localFileClient.readDirectory(nonExistentDir, "*.md"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("目錄不存在");
    }

    @Test
    @DisplayName("當文件不存在時應拋出例外")
    void shouldThrowExceptionWhenFileNotExists() {
        // Arrange
        Path nonExistentFile = tempDir.resolve("non-existent.md");

        // Act & Assert
        assertThatThrownBy(() -> localFileClient.readFile(nonExistentFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("文件不存在");
    }

    @Test
    @DisplayName("應列出符合模式的文件路徑")
    void shouldListFilesMatchingPattern() throws IOException {
        // Arrange
        Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(tempDir.resolve("docs/guide.md"), "# Guide");
        Files.writeString(tempDir.resolve("docs/api.md"), "# API");
        Files.writeString(tempDir.resolve("readme.txt"), "README");

        // Act - **/*.md 只匹配子目錄中的文件
        var paths = localFileClient.listFiles(tempDir, "**/*.md");

        // Assert
        assertThat(paths).hasSize(2);
        assertThat(paths).containsExactlyInAnyOrder("docs/guide.md", "docs/api.md");
    }

    @Test
    @DisplayName("當沒有符合模式的文件時應回傳空列表")
    void shouldReturnEmptyListWhenNoMatchingFiles() throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("file.txt"), "Not markdown");

        // Act
        var files = localFileClient.readDirectory(tempDir, "*.md");

        // Assert
        assertThat(files).isEmpty();
    }
}
