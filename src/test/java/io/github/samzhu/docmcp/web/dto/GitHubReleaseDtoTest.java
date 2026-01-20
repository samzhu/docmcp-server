package io.github.samzhu.docmcp.web.dto;

import io.github.samzhu.docmcp.infrastructure.github.GitHubRelease;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GitHubReleaseDto 單元測試
 * <p>
 * 測試從 GitHubRelease 轉換為 GitHubReleaseDto 的各種情況
 * </p>
 */
@DisplayName("GitHubReleaseDto")
class GitHubReleaseDtoTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final String DOCS_PATH = "docs";

    @Nested
    @DisplayName("from() 轉換方法")
    class FromMethodTests {

        @Test
        @DisplayName("應正確轉換有 name 的 Release")
        void shouldConvertReleaseWithName() {
            // Given: 有完整 name 的 Release
            GitHubRelease release = new GitHubRelease(
                    1L,
                    "v4.0.1",
                    "Spring Boot 4.0.1",
                    "Release notes",
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, false, DOCS_PATH);

            // Then
            assertThat(dto.tagName()).isEqualTo("v4.0.1");
            assertThat(dto.version()).isEqualTo("4.0.1");
            assertThat(dto.name()).isEqualTo("Spring Boot 4.0.1");
            assertThat(dto.exists()).isFalse();
            assertThat(dto.docsPath()).isEqualTo(DOCS_PATH);
        }

        @Test
        @DisplayName("當 name 為 null 時應使用 tagName 作為 fallback")
        void shouldUseTagNameWhenNameIsNull() {
            // Given: name 為 null 的 Release（GitHub 有時候只有 tag 沒有 release name）
            GitHubRelease release = new GitHubRelease(
                    2L,
                    "v3.5.9",
                    null,  // name 為 null
                    null,
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, false, DOCS_PATH);

            // Then: 應使用 tagName 作為 name 的 fallback
            assertThat(dto.tagName()).isEqualTo("v3.5.9");
            assertThat(dto.version()).isEqualTo("3.5.9");
            assertThat(dto.name())
                    .as("當 name 為 null 時應使用 tagName 作為 fallback")
                    .isEqualTo("v3.5.9");
        }

        @Test
        @DisplayName("當 name 為空字串時應使用 tagName 作為 fallback")
        void shouldUseTagNameWhenNameIsEmpty() {
            // Given: name 為空字串的 Release
            GitHubRelease release = new GitHubRelease(
                    3L,
                    "v3.4.13",
                    "",  // name 為空字串
                    null,
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, false, DOCS_PATH);

            // Then: 應使用 tagName 作為 name 的 fallback
            assertThat(dto.name())
                    .as("當 name 為空字串時應使用 tagName 作為 fallback")
                    .isEqualTo("v3.4.13");
        }

        @Test
        @DisplayName("當 name 只有空白時應使用 tagName 作為 fallback")
        void shouldUseTagNameWhenNameIsBlank() {
            // Given: name 只有空白的 Release
            GitHubRelease release = new GitHubRelease(
                    4L,
                    "v3.3.0",
                    "   ",  // name 只有空白
                    null,
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, false, DOCS_PATH);

            // Then: 應使用 tagName 作為 name 的 fallback
            assertThat(dto.name())
                    .as("當 name 只有空白時應使用 tagName 作為 fallback")
                    .isEqualTo("v3.3.0");
        }

        @Test
        @DisplayName("應正確處理沒有 v 前綴的版本號")
        void shouldHandleVersionWithoutVPrefix() {
            // Given: 沒有 v 前綴的 tagName
            GitHubRelease release = new GitHubRelease(
                    5L,
                    "1.0.0",
                    null,
                    null,
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, false, DOCS_PATH);

            // Then
            assertThat(dto.tagName()).isEqualTo("1.0.0");
            assertThat(dto.version()).isEqualTo("1.0.0");
            assertThat(dto.name()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("應正確標記已存在的版本")
        void shouldMarkExistingVersion() {
            // Given
            GitHubRelease release = new GitHubRelease(
                    6L,
                    "v4.0.0",
                    "Spring Boot 4.0.0",
                    null,
                    false,
                    false,
                    NOW,
                    "https://tarball",
                    "https://zipball"
            );

            // When
            GitHubReleaseDto dto = GitHubReleaseDto.from(release, true, DOCS_PATH);

            // Then
            assertThat(dto.exists()).isTrue();
        }
    }
}
