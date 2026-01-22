package io.github.samzhu.docmcp.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試所有 Entity 的 @Version 樂觀鎖定邏輯
 * <p>
 * 這是重要的 TDD 保護：確保 Spring Data JDBC 能正確判斷新實體執行 INSERT，
 * 已存在實體執行 UPDATE。
 * </p>
 * <p>
 * 使用 @Version 欄位判斷：
 * - version = null → INSERT（新實體）
 * - version != null → UPDATE（既有實體）
 * </p>
 */
class VersionEntityTest {

    /**
     * 產生隨機 TSID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Nested
    @DisplayName("Library @Version 測試")
    class LibraryVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var library = Library.create(
                    randomId(),
                    "spring-boot",
                    "Spring Boot",
                    null,
                    SourceType.GITHUB,
                    null,
                    null,
                    null
            );

            assertThat(library.getVersion()).isNull();
            assertThat(library.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var library = new Library(
                    randomId(),
                    "spring-boot",
                    "Spring Boot",
                    null,
                    SourceType.GITHUB,
                    null,
                    null,
                    List.of(),
                    0L,  // version 有值
                    OffsetDateTime.now(),
                    null
            );

            assertThat(library.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("LibraryVersion @Version 測試")
    class LibraryVersionVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 entityVersion 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var version = LibraryVersion.create(randomId(), randomId(), "1.0.0", true);

            assertThat(version.getEntityVersion()).isNull();
            assertThat(version.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("entityVersion 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var version = new LibraryVersion(
                    randomId(),
                    randomId(),
                    "1.0.0",
                    true,
                    false,
                    VersionStatus.ACTIVE,
                    null,
                    null,
                    0L,  // entityVersion 有值
                    OffsetDateTime.now(),
                    null
            );

            assertThat(version.getEntityVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Document @Version 測試")
    class DocumentVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var document = Document.create(
                    randomId(),
                    randomId(),
                    "Title",
                    "/path/to/doc.md",
                    "content",
                    "hash123",
                    "markdown"
            );

            assertThat(document.getVersion()).isNull();
            assertThat(document.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var document = new Document(
                    randomId(),
                    randomId(),
                    "Title",
                    "/path/to/doc.md",
                    "content",
                    "hash123",
                    "markdown",
                    Map.of(),
                    0L,  // version 有值
                    OffsetDateTime.now(),
                    null
            );

            assertThat(document.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("DocumentChunk @Version 測試")
    class DocumentChunkVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var chunk = DocumentChunk.create(
                    randomId(),
                    randomId(),
                    0,
                    "chunk content",
                    new float[768],
                    100
            );

            assertThat(chunk.getVersion()).isNull();
            assertThat(chunk.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var now = OffsetDateTime.now();
            var chunk = new DocumentChunk(
                    randomId(),
                    randomId(),
                    0,
                    "chunk content",
                    new float[768],
                    100,
                    Map.of(),
                    0L,  // version 有值
                    now,
                    now
            );

            assertThat(chunk.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("CodeExample @Version 測試")
    class CodeExampleVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var codeExample = CodeExample.create(
                    randomId(),
                    randomId(),
                    "java",
                    "System.out.println(\"Hello\");",
                    "Print hello"
            );

            assertThat(codeExample.getVersion()).isNull();
            assertThat(codeExample.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var now = OffsetDateTime.now();
            var codeExample = new CodeExample(
                    randomId(),
                    randomId(),
                    "java",
                    "System.out.println(\"Hello\");",
                    "Print hello",
                    1,
                    5,
                    Map.of(),
                    0L,  // version 有值
                    now,
                    now
            );

            assertThat(codeExample.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("SyncHistory @Version 測試")
    class SyncHistoryVersionTest {

        @Test
        @DisplayName("使用 createPending() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var syncHistory = SyncHistory.createPending(randomId(), randomId());

            assertThat(syncHistory.getVersion()).isNull();
            assertThat(syncHistory.getCreatedAt()).isNull();
            assertThat(syncHistory.getStartedAt()).isNotNull();  // 應用層設定
            assertThat(syncHistory.getStatus()).isEqualTo(SyncStatus.PENDING);
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var now = OffsetDateTime.now();
            var syncHistory = new SyncHistory(
                    randomId(),
                    randomId(),
                    SyncStatus.RUNNING,
                    now,
                    null,
                    0,
                    0,
                    null,
                    Map.of(),
                    0L,  // version 有值
                    now,
                    now
            );

            assertThat(syncHistory.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("更新 SyncHistory 時應保留 version 以進行樂觀鎖定")
        void shouldPreserveVersionWhenUpdatingExistingRecord() {
            var now = OffsetDateTime.now();
            // 模擬從資料庫讀取的記錄（version 有值）
            var existing = new SyncHistory(
                    randomId(),
                    randomId(),
                    SyncStatus.PENDING,
                    now.minusMinutes(5),
                    null,
                    0,
                    0,
                    null,
                    Map.of(),
                    0L,  // 資料庫設定的 version
                    now.minusMinutes(5),
                    now.minusMinutes(5)
            );

            // 模擬更新為 RUNNING 狀態
            var updated = new SyncHistory(
                    existing.getId(),
                    existing.getVersionId(),
                    SyncStatus.RUNNING,
                    existing.getStartedAt(),
                    null,
                    0,
                    0,
                    null,
                    Map.of(),
                    existing.getVersion(),  // 保留原始 version 以進行樂觀鎖定
                    existing.getCreatedAt(),
                    existing.getUpdatedAt()
            );

            assertThat(updated.getVersion()).isEqualTo(0L);
            assertThat(updated.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ApiKey @Version 測試")
    class ApiKeyVersionTest {

        @Test
        @DisplayName("使用 create() 建立的實體 version 應為 null（新實體）")
        void shouldHaveNullVersionWhenCreatedWithFactoryMethod() {
            var apiKey = ApiKey.create(
                    randomId(),
                    "Test Key",
                    "hash",
                    "dmcp_test",
                    1000,
                    null,
                    "admin"
            );

            assertThat(apiKey.getVersion()).isNull();
            assertThat(apiKey.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("version 有值時應為既有實體")
        void shouldHaveVersionWhenLoadedFromDatabase() {
            var now = OffsetDateTime.now();
            var apiKey = new ApiKey(
                    randomId(),
                    "Test Key",
                    "hash",
                    "dmcp_test",
                    ApiKeyStatus.ACTIVE,
                    1000,
                    null,
                    null,
                    "admin",
                    0L,  // version 有值
                    now,
                    now
            );

            assertThat(apiKey.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("withLastUsedAt() 應保留 version 以進行樂觀鎖定")
        void shouldPreserveVersionWhenCallingWithLastUsedAt() {
            var now = OffsetDateTime.now();
            var apiKey = new ApiKey(
                    randomId(),
                    "Test Key",
                    "hash",
                    "dmcp_test",
                    ApiKeyStatus.ACTIVE,
                    1000,
                    null,
                    null,
                    "admin",
                    0L,  // version 有值
                    now,
                    now
            );

            var updated = apiKey.withLastUsedAt(OffsetDateTime.now());

            assertThat(updated.getVersion()).isEqualTo(0L);  // version 應被保留
        }

        @Test
        @DisplayName("revoke() 應保留 version 以進行樂觀鎖定")
        void shouldPreserveVersionWhenCallingRevoke() {
            var now = OffsetDateTime.now();
            var apiKey = new ApiKey(
                    randomId(),
                    "Test Key",
                    "hash",
                    "dmcp_test",
                    ApiKeyStatus.ACTIVE,
                    1000,
                    null,
                    null,
                    "admin",
                    0L,  // version 有值
                    now,
                    now
            );

            var revoked = apiKey.revoke();

            assertThat(revoked.getVersion()).isEqualTo(0L);  // version 應被保留
            assertThat(revoked.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        }
    }
}
