package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.config.FeatureFlags;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.GetMigrationGuideResult;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GetMigrationGuideTool 單元測試
 */
@DisplayName("GetMigrationGuideTool")
class GetMigrationGuideToolTest {

    private FeatureFlags featureFlags;
    private LibraryVersionRepository versionRepository;
    private DocumentRepository documentRepository;
    private GetMigrationGuideTool getMigrationGuideTool;

    @BeforeEach
    void setUp() {
        featureFlags = mock(FeatureFlags.class);
        versionRepository = mock(LibraryVersionRepository.class);
        documentRepository = mock(DocumentRepository.class);
        getMigrationGuideTool = new GetMigrationGuideTool(featureFlags, versionRepository, documentRepository);
    }

    @Test
    @DisplayName("should return disabled message when feature is disabled")
    void shouldReturnDisabledMessageWhenFeatureIsDisabled() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(false);
        UUID libraryId = UUID.randomUUID();

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.libraryId()).isEqualTo(libraryId);
        assertThat(result.fromVersion()).isEqualTo("3.0.0");
        assertThat(result.toVersion()).isEqualTo("3.5.0");
        assertThat(result.guides()).isEmpty();
        assertThat(result.message()).contains("currently disabled");
    }

    @Test
    @DisplayName("should return error when fromVersion is blank")
    void shouldReturnErrorWhenFromVersionIsBlank() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "", "3.5.0");

        // Assert
        assertThat(result.guides()).isEmpty();
        assertThat(result.message()).contains("required");
    }

    @Test
    @DisplayName("should return error when toVersion is blank")
    void shouldReturnErrorWhenToVersionIsBlank() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "");

        // Assert
        assertThat(result.guides()).isEmpty();
        assertThat(result.message()).contains("required");
    }

    @Test
    @DisplayName("should return empty result when no versions found")
    void shouldReturnEmptyResultWhenNoVersionsFound() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of());

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).isEmpty();
        assertThat(result.message()).contains("No migration guides found");
    }

    @Test
    @DisplayName("should find migration guide by title")
    void shouldFindMigrationGuideByTitle() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        var migrationDoc = createDocument(UUID.randomUUID(), versionId,
                "Migration Guide from 3.0 to 3.5",
                "docs/migration.md",
                "This guide helps you migrate...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(migrationDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(1);
        assertThat(result.guides().getFirst().title()).contains("Migration");
        assertThat(result.guides().getFirst().relevance()).isEqualTo("migration");
    }

    @Test
    @DisplayName("should find upgrade guide by path")
    void shouldFindUpgradeGuideByPath() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        var upgradeDoc = createDocument(UUID.randomUUID(), versionId,
                "Version 3.5 Guide",
                "docs/upgrading-to-3.5.md",
                "Steps to upgrade your application...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(upgradeDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(1);
        assertThat(result.guides().getFirst().relevance()).isEqualTo("upgrade");
    }

    @Test
    @DisplayName("should find breaking changes document by content")
    void shouldFindBreakingChangesDocumentByContent() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        // Use a title and path that doesn't match other keywords
        var breakingDoc = createDocument(UUID.randomUUID(), versionId,
                "Version 3.5 Notes",
                "docs/notes.md",
                "## Breaking Changes\n\nThe following APIs have been removed...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(breakingDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(1);
        assertThat(result.guides().getFirst().relevance()).isEqualTo("breaking-changes");
    }

    @Test
    @DisplayName("should find changelog document")
    void shouldFindChangelogDocument() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        var changelogDoc = createDocument(UUID.randomUUID(), versionId,
                "Changelog",
                "CHANGELOG.md",
                "## [3.5.0] - 2024-01-15\n### Added\n- New feature...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(changelogDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(1);
        assertThat(result.guides().getFirst().relevance()).isEqualTo("release-notes");
    }

    @Test
    @DisplayName("should not include unrelated documents")
    void shouldNotIncludeUnrelatedDocuments() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        var unrelatedDoc = createDocument(UUID.randomUUID(), versionId,
                "Getting Started",
                "docs/getting-started.md",
                "Welcome to our library! This guide will help you get started...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(unrelatedDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).isEmpty();
        assertThat(result.message()).contains("No migration guides found");
    }

    @Test
    @DisplayName("should sort guides by relevance priority")
    void shouldSortGuidesByRelevancePriority() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        var changelogDoc = createDocument(UUID.randomUUID(), versionId,
                "Changelog", "CHANGELOG.md", "Changes...");
        var migrationDoc = createDocument(UUID.randomUUID(), versionId,
                "Migration Guide", "docs/migration.md", "Migrate...");
        var upgradeDoc = createDocument(UUID.randomUUID(), versionId,
                "Upgrade Steps", "docs/upgrade.md", "Upgrade...");

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId))
                .thenReturn(List.of(changelogDoc, migrationDoc, upgradeDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(3);
        // Migration should be first (priority 1)
        assertThat(result.guides().get(0).relevance()).isEqualTo("migration");
        // Upgrade should be second (priority 2)
        assertThat(result.guides().get(1).relevance()).isEqualTo("upgrade");
        // Release notes should be last (priority 4)
        assertThat(result.guides().get(2).relevance()).isEqualTo("release-notes");
    }

    @Test
    @DisplayName("should truncate long content in excerpt")
    void shouldTruncateLongContentInExcerpt() {
        // Arrange
        when(featureFlags.isMigrationGuides()).thenReturn(true);
        UUID libraryId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        var version = createVersion(versionId, libraryId, "3.5.0");
        String longContent = "Migration guide content. ".repeat(50);
        var migrationDoc = createDocument(UUID.randomUUID(), versionId,
                "Migration Guide", "docs/migration.md", longContent);

        when(versionRepository.findByLibraryId(libraryId)).thenReturn(List.of(version));
        when(documentRepository.findByVersionId(versionId)).thenReturn(List.of(migrationDoc));

        // Act
        GetMigrationGuideResult result = getMigrationGuideTool.getMigrationGuide(
                libraryId, "3.0.0", "3.5.0");

        // Assert
        assertThat(result.guides()).hasSize(1);
        assertThat(result.guides().getFirst().excerpt()).hasSizeLessThanOrEqualTo(303); // 300 + "..."
        assertThat(result.guides().getFirst().excerpt()).endsWith("...");
    }

    // Helper methods
    private LibraryVersion createVersion(UUID id, UUID libraryId, String version) {
        return new LibraryVersion(id, libraryId, version, true, false,
                VersionStatus.ACTIVE, "docs", null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private Document createDocument(UUID id, UUID versionId, String title, String path, String content) {
        return new Document(id, versionId, title, path, content, "hash",
                "markdown", Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
    }
}
