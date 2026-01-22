package io.github.samzhu.docmcp.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LibraryVersion 實體測試
 */
class LibraryVersionTest {

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    void shouldCreateVersionWithRequiredFields() {
        // 測試使用必要欄位建立版本
        var libraryId = randomId();
        var version = LibraryVersion.create(randomId(), libraryId, "3.2.0", true);

        assertThat(version.getLibraryId()).isEqualTo(libraryId);
        assertThat(version.getVersion()).isEqualTo("3.2.0");
        assertThat(version.getIsLatest()).isTrue();
        assertThat(version.getStatus()).isEqualTo(VersionStatus.ACTIVE); // 預設為 ACTIVE
    }

    @Test
    void shouldCreateNonLatestVersion() {
        // 測試建立非最新版本
        var libraryId = randomId();
        var version = LibraryVersion.create(randomId(), libraryId, "3.1.0", false);

        assertThat(version.getIsLatest()).isFalse();
    }

    @Test
    void shouldCreateVersionWithIsLtsField() {
        // 測試建立帶有 LTS 標記的版本
        var libraryId = randomId();
        var version = LibraryVersion.create(randomId(), libraryId, "3.2.0", true);

        // 預設 isLts 應為 false
        assertThat(version.getIsLts()).isFalse();
    }

    @Test
    void shouldCreateLtsVersionWithCreateLts() {
        // 測試使用 createLts 建立 LTS 版本
        var libraryId = randomId();
        var version = LibraryVersion.createLts(randomId(), libraryId, "3.2.0", true);

        assertThat(version.getIsLts()).isTrue();
        assertThat(version.getIsLatest()).isTrue();
        assertThat(version.getStatus()).isEqualTo(VersionStatus.ACTIVE);
    }
}
