package io.github.samzhu.docmcp.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SyncService;
import io.github.samzhu.docmcp.web.dto.CreateLibraryRequest;
import io.github.samzhu.docmcp.web.dto.UpdateLibraryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LibraryApiController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class LibraryApiControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 產生隨機 ID
     */
    private String randomId() {
        return TsidCreator.getTsid().toString();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private LibraryService libraryService;

    @MockitoBean
    private SyncService syncService;

    @Test
    @WithMockUser
    void shouldListAllLibraries() throws Exception {
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("spring-boot", "Spring Boot", "backend")
        );
        when(libraryService.listLibraries(null)).thenReturn(libraries);

        mockMvc.perform(get("/api/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("react"))
                .andExpect(jsonPath("$[1].name").value("spring-boot"));
    }

    @Test
    @WithMockUser
    void shouldListLibrariesByCategory() throws Exception {
        var libraries = List.of(
                createLibrary("react", "React", "frontend"),
                createLibrary("vue", "Vue.js", "frontend")
        );
        when(libraryService.listLibraries("frontend")).thenReturn(libraries);

        mockMvc.perform(get("/api/libraries").param("category", "frontend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser
    void shouldCreateLibrary() throws Exception {
        var request = new CreateLibraryRequest(
                "react",
                "React",
                "A JavaScript library",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of("javascript", "ui")
        );
        var library = createLibrary("react", "React", "frontend");
        when(libraryService.createLibrary(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(library);

        mockMvc.perform(post("/api/libraries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("react"));
    }

    @Test
    @WithMockUser
    void shouldGetLibraryById() throws Exception {
        var library = createLibrary("react", "React", "frontend");
        when(libraryService.getLibraryById(library.getId())).thenReturn(library);

        mockMvc.perform(get("/api/libraries/{id}", library.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("react"))
                .andExpect(jsonPath("$.displayName").value("React"));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenLibraryNotFound() throws Exception {
        var id = randomId();
        when(libraryService.getLibraryById(id)).thenThrow(LibraryNotFoundException.byId(id));

        mockMvc.perform(get("/api/libraries/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Library Not Found"));
    }

    @Test
    @WithMockUser
    void shouldUpdateLibrary() throws Exception {
        var id = randomId();
        var request = new UpdateLibraryRequest(
                "React Updated",
                "Updated description",
                null,
                null,
                null,
                null
        );
        var library = new Library(
                id, "react", "React Updated", "Updated description",
                SourceType.GITHUB, null, "frontend", null, 0L, null, null
        );
        when(libraryService.updateLibrary(eq(id), any(), any(), any(), any(), any(), any()))
                .thenReturn(library);

        mockMvc.perform(put("/api/libraries/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("React Updated"));
    }

    @Test
    @WithMockUser
    void shouldDeleteLibrary() throws Exception {
        var id = randomId();
        doNothing().when(libraryService).deleteLibrary(id);

        mockMvc.perform(delete("/api/libraries/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(libraryService).deleteLibrary(id);
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenDeleteNonexistentLibrary() throws Exception {
        var id = randomId();
        doThrow(LibraryNotFoundException.byId(id)).when(libraryService).deleteLibrary(id);

        mockMvc.perform(delete("/api/libraries/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldListLibraryVersions() throws Exception {
        var libraryId = randomId();
        var versions = List.of(
                createVersion(libraryId, "3.2.0", true),
                createVersion(libraryId, "3.1.0", false)
        );
        when(libraryService.getLibraryVersionsById(libraryId)).thenReturn(versions);

        mockMvc.perform(get("/api/libraries/{id}/versions", libraryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value("3.2.0"));
    }

    @Test
    @WithMockUser
    void shouldReturn400WhenCreatingLibraryWithInvalidName() throws Exception {
        var request = new CreateLibraryRequest(
                "Invalid Name!",  // 無效的名稱（包含空格和特殊字元）
                "Test",
                null,
                SourceType.GITHUB,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/libraries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Library createLibrary(String name, String displayName, String category) {
        return new Library(
                randomId(),
                name,
                displayName,
                null,
                SourceType.GITHUB,
                "https://github.com/test/" + name,
                category,
                null,
                0L,                     // version（模擬從資料庫讀取）
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private LibraryVersion createVersion(String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(
                randomId(),
                libraryId,
                version,
                isLatest,
                false,
                VersionStatus.ACTIVE,
                "docs",
                null,
                0L,                     // entityVersion（模擬從資料庫讀取）
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
