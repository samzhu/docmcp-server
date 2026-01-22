package io.github.samzhu.docmcp.domain.model;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Library 實體測試
 */
class LibraryTest {

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @Test
    void shouldCreateLibraryWithRequiredFields() {
        // 測試使用必要欄位建立函式庫
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

        assertThat(library.getName()).isEqualTo("spring-boot");
        assertThat(library.getDisplayName()).isEqualTo("Spring Boot");
        assertThat(library.getSourceType()).isEqualTo(SourceType.GITHUB);
        assertThat(library.getId()).isNotNull();
    }

    @Test
    void shouldCreateLibraryWithAllFields() {
        // 測試使用所有欄位建立函式庫
        var library = Library.create(
                randomId(),
                "react",
                "React",
                "A JavaScript library for building user interfaces",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of("javascript", "ui", "framework")
        );

        assertThat(library.getName()).isEqualTo("react");
        assertThat(library.getDescription()).isEqualTo("A JavaScript library for building user interfaces");
        assertThat(library.getSourceUrl()).isEqualTo("https://github.com/facebook/react");
        assertThat(library.getCategory()).isEqualTo("frontend");
        assertThat(library.getTags()).containsExactly("javascript", "ui", "framework");
    }
}
