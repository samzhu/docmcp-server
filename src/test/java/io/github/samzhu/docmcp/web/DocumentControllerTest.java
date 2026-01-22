package io.github.samzhu.docmcp.web;

import com.github.f4b6a3.tsid.TsidCreator;
import io.github.samzhu.docmcp.TestConfig;
import io.github.samzhu.docmcp.TestcontainersConfiguration;
import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.domain.enums.VersionStatus;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.DocumentService;
import io.github.samzhu.docmcp.service.LibraryService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentController 測試
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestConfig.class})
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private DocumentRepository documentRepository;

    @MockitoBean
    private LibraryService libraryService;

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

    @Test
    @WithMockUser
    void shouldShowDocumentListPage() throws Exception {
        // Arrange
        String versionId = randomId();
        String libraryId = randomId();
        String docId = randomId();

        Library library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        LibraryVersion version = createVersion(versionId, libraryId, "3.2.0", true);
        Document document = createDocument(docId, versionId, "Getting Started", "docs/getting-started.md");

        when(libraryService.getVersionById(versionId)).thenReturn(version);
        when(libraryService.getLibraryById(libraryId)).thenReturn(library);
        when(documentRepository.findByVersionIdOrderByPathAsc(versionId)).thenReturn(List.of(document));

        // Act & Assert
        mockMvc.perform(get("/documents").param("versionId", versionId))
                .andExpect(status().isOk())
                .andExpect(view().name("documents/list"))
                .andExpect(model().attributeExists("documents"))
                .andExpect(model().attributeExists("library"))
                .andExpect(model().attributeExists("version"));
    }

    @Test
    @WithMockUser
    void shouldShowDocumentDetailPage() throws Exception {
        // Arrange
        String docId = randomId();
        String versionId = randomId();
        String libraryId = randomId();

        Library library = createLibrary(libraryId, "spring-boot", "Spring Boot");
        LibraryVersion version = createVersion(versionId, libraryId, "3.2.0", true);
        Document document = createDocument(docId, versionId, "Getting Started", "docs/getting-started.md");

        when(documentService.getDocument(docId)).thenReturn(Optional.of(document));
        when(libraryService.getVersionById(versionId)).thenReturn(version);
        when(libraryService.getLibraryById(libraryId)).thenReturn(library);

        // Act & Assert
        mockMvc.perform(get("/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(view().name("documents/detail"))
                .andExpect(model().attributeExists("document"))
                .andExpect(model().attributeExists("library"))
                .andExpect(model().attributeExists("version"));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        // Arrange
        String docId = randomId();
        when(documentService.getDocument(docId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/documents/{id}", docId))
                .andExpect(status().isNotFound());
    }

    private Library createLibrary(String id, String name, String displayName) {
        return new Library(id, name, displayName, null, SourceType.GITHUB, null, null, null, 0L, null, null);
    }

    private LibraryVersion createVersion(String id, String libraryId, String version, boolean isLatest) {
        return new LibraryVersion(id, libraryId, version, isLatest, false, VersionStatus.ACTIVE, "docs", null, 0L, null, null);
    }

    private Document createDocument(String id, String versionId, String title, String path) {
        return new Document(id, versionId, title, path, "# Getting Started\n\nWelcome!",
                null, "markdown", Map.of(), 0L, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
