package io.github.samzhu.docmcp.domain.model;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Library 實體測試
 */
class LibraryTest {

    @Test
    void shouldCreateLibraryWithRequiredFields() {
        // 測試使用必要欄位建立函式庫
        var library = Library.create(
                "spring-boot",
                "Spring Boot",
                null,
                SourceType.GITHUB,
                null,
                null,
                null
        );

        assertThat(library.name()).isEqualTo("spring-boot");
        assertThat(library.displayName()).isEqualTo("Spring Boot");
        assertThat(library.sourceType()).isEqualTo(SourceType.GITHUB);
        assertThat(library.id()).isNull(); // ID 由資料庫產生
    }

    @Test
    void shouldCreateLibraryWithAllFields() {
        // 測試使用所有欄位建立函式庫
        var library = Library.create(
                "react",
                "React",
                "A JavaScript library for building user interfaces",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of("javascript", "ui", "framework")
        );

        assertThat(library.name()).isEqualTo("react");
        assertThat(library.description()).isEqualTo("A JavaScript library for building user interfaces");
        assertThat(library.sourceUrl()).isEqualTo("https://github.com/facebook/react");
        assertThat(library.category()).isEqualTo("frontend");
        assertThat(library.tags()).containsExactly("javascript", "ui", "framework");
    }
}
