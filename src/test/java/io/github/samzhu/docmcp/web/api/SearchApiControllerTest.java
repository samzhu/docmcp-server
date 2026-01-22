package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.github.f4b6a3.tsid.TsidCreator;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SearchApiController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class SearchApiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private LibraryService libraryService;

    @Test
    @WithMockUser
    void shouldSearchWithFulltextMode() throws Exception {
        var libraryId = TsidCreator.getTsid().toString();
        var results = List.of(
                SearchResultItem.fromDocument(
                        TsidCreator.getTsid().toString(), "Getting Started", "/docs/getting-started.md",
                        "How to get started with...", 1.0
                )
        );
        when(searchService.fullTextSearch(eq(libraryId), any(), eq("getting started"), eq(10)))
                .thenReturn(results);

        mockMvc.perform(get("/api/search")
                        .param("query", "getting started")
                        .param("libraryId", libraryId.toString())
                        .param("mode", "fulltext"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("getting started"))
                .andExpect(jsonPath("$.mode").value("fulltext"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Getting Started"));
    }

    @Test
    @WithMockUser
    void shouldSearchWithSemanticMode() throws Exception {
        var libraryId = TsidCreator.getTsid().toString();
        var results = List.of(
                SearchResultItem.fromChunk(
                        TsidCreator.getTsid().toString(), TsidCreator.getTsid().toString(),
                        "Configuration Guide", "/docs/config.md",
                        "Configure your application...", 0.85, 0
                )
        );
        when(searchService.semanticSearch(eq(libraryId), any(), eq("how to configure"), eq(10), anyDouble()))
                .thenReturn(results);

        mockMvc.perform(get("/api/search")
                        .param("query", "how to configure")
                        .param("libraryId", libraryId.toString())
                        .param("mode", "semantic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("semantic"))
                .andExpect(jsonPath("$.items[0].score").value(0.85));
    }

    @Test
    @WithMockUser
    void shouldSearchWithHybridMode() throws Exception {
        // 測試 hybrid 模式：現在使用 RRF 演算法的 hybridSearch 方法
        var libraryId = TsidCreator.getTsid().toString();
        var library = new Library(
                libraryId, "spring-boot", "Spring Boot", null,
                SourceType.GITHUB, null, "backend", null, 0L, null, null
        );
        // RRF 演算法合併後的結果
        var hybridResults = List.of(
                SearchResultItem.fromDocument(
                        TsidCreator.getTsid().toString(), "Quick Start", "/docs/quick-start.md",
                        "Quick start guide...", 0.0164
                ),
                SearchResultItem.fromChunk(
                        TsidCreator.getTsid().toString(), TsidCreator.getTsid().toString(),
                        "Introduction", "/docs/intro.md",
                        "Introduction to...", 0.0115, 0
                )
        );

        when(libraryService.listLibraries(null)).thenReturn(List.of(library));
        when(searchService.hybridSearch(eq(libraryId), any(), eq("quick start"), eq(10)))
                .thenReturn(hybridResults);

        mockMvc.perform(get("/api/search")
                        .param("query", "quick start")
                        .param("mode", "hybrid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("hybrid"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyResultsForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("query", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyResultsWhenNoLibraries() throws Exception {
        when(libraryService.listLibraries(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/search")
                        .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @WithMockUser
    void shouldReturn400ForInvalidSearchMode() throws Exception {
        var libraryId = TsidCreator.getTsid().toString();

        mockMvc.perform(get("/api/search")
                        .param("query", "test")
                        .param("libraryId", libraryId.toString())
                        .param("mode", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldApplyCustomLimit() throws Exception {
        var libraryId = TsidCreator.getTsid().toString();
        when(searchService.fullTextSearch(eq(libraryId), any(), any(), eq(5)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/search")
                        .param("query", "test")
                        .param("libraryId", libraryId.toString())
                        .param("mode", "fulltext")
                        .param("limit", "5"))
                .andExpect(status().isOk());
    }
}
