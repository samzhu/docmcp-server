package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LibraryVersion 實體測試
 */
class LibraryVersionTest {

    @Test
    void shouldCreateVersionWithRequiredFields() {
        // 測試使用必要欄位建立版本
        var libraryId = UUID.randomUUID();
        var version = LibraryVersion.create(libraryId, "3.2.0", true);

        assertThat(version.libraryId()).isEqualTo(libraryId);
        assertThat(version.version()).isEqualTo("3.2.0");
        assertThat(version.isLatest()).isTrue();
        assertThat(version.status()).isEqualTo(VersionStatus.ACTIVE); // 預設為 ACTIVE
    }

    @Test
    void shouldCreateNonLatestVersion() {
        // 測試建立非最新版本
        var libraryId = UUID.randomUUID();
        var version = LibraryVersion.create(libraryId, "3.1.0", false);

        assertThat(version.isLatest()).isFalse();
    }

    @Test
    void shouldCreateVersionWithIsLtsField() {
        // 測試建立帶有 LTS 標記的版本
        var libraryId = UUID.randomUUID();
        var version = LibraryVersion.create(libraryId, "3.2.0", true);

        // 預設 isLts 應為 false
        assertThat(version.isLts()).isFalse();
    }

    @Test
    void shouldCreateLtsVersionWithCreateLts() {
        // 測試使用 createLts 建立 LTS 版本
        var libraryId = UUID.randomUUID();
        var version = LibraryVersion.createLts(libraryId, "3.2.0", true);

        assertThat(version.isLts()).isTrue();
        assertThat(version.isLatest()).isTrue();
        assertThat(version.status()).isEqualTo(VersionStatus.ACTIVE);
    }
}
