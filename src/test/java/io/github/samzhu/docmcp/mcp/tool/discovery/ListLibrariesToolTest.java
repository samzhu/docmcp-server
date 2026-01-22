package io.github.samzhu.docmcp.mcp.tool.discovery;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ListLibrariesTool 單元測試
 */
@ExtendWith(MockitoExtension.class)
class ListLibrariesToolTest {

    @Mock
    private LibraryService libraryService;

    private ListLibrariesTool listLibrariesTool;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        listLibrariesTool = new ListLibrariesTool(libraryService);
    }

    @Test
    void shouldReturnAllLibraries() {
        // 測試回傳所有函式庫
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("spring-boot", "Spring Boot", "backend")
        );
        when(libraryService.listLibraries(null)).thenReturn(libraries);

        var result = listLibrariesTool.listLibraries(null);

        assertThat(result.libraries()).hasSize(2);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void shouldReturnLibrariesByCategory() {
        // 測試依分類篩選函式庫
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("vue", "Vue.js", "frontend")
        );
        when(libraryService.listLibraries("frontend")).thenReturn(libraries);

        var result = listLibrariesTool.listLibraries("frontend");

        assertThat(result.libraries()).hasSize(2);
        assertThat(result.libraries())
                .allMatch(lib -> lib.category().equals("frontend"));
    }

    @Test
    void shouldReturnEmptyListWhenNoLibraries() {
        // 測試當沒有函式庫時回傳空列表
        when(libraryService.listLibraries(null)).thenReturn(List.of());

        var result = listLibrariesTool.listLibraries(null);

        assertThat(result.libraries()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
    }

    /**
     * 建立測試用的函式庫
     */
    private Library createLibrary(String name, String displayName, String category) {
        return new Library(
                randomId(),
                name,
                displayName,
                null,
                SourceType.GITHUB,
                null,
                category,
                null,
                0L,     // version（模擬從資料庫讀取）
                null,
                null
        );
    }
}
