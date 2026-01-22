package io.github.samzhu.docmcp.mcp.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.docmcp.service.LibraryService;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 函式庫元資料資源提供者
 * <p>
 * 提供 MCP Resource 存取函式庫元資料。
 * URI 格式: library://{libraryId}
 * </p>
 */
@Component
public class LibraryResourceProvider {

    private final LibraryService libraryService;
    private final ObjectMapper objectMapper;

    public LibraryResourceProvider(LibraryService libraryService, ObjectMapper objectMapper) {
        this.libraryService = libraryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 取得函式庫元資料
     * <p>
     * 透過 URI 模板存取函式庫的詳細資訊，包含版本列表。
     * </p>
     *
     * @param libraryId 函式庫 ID
     * @return 函式庫元資料（JSON 格式）
     */
    @McpResource(
            uri = "library://{libraryId}",
            name = "Library Metadata",
            description = "取得函式庫的元資料，包含名稱、描述、版本列表等資訊",
            mimeType = "application/json"
    )
    public ReadResourceResult getLibraryResource(
            String libraryId
    ) {
        var library = libraryService.getLibraryById(libraryId);
        var versions = libraryService.getLibraryVersionsById(libraryId);

        // 建立回傳資料結構
        var metadata = Map.of(
                "id", library.getId(),
                "name", library.getName(),
                "displayName", library.getDisplayName() != null ? library.getDisplayName() : library.getName(),
                "description", library.getDescription() != null ? library.getDescription() : "",
                "category", library.getCategory() != null ? library.getCategory() : "",
                "tags", library.getTags() != null ? library.getTags() : List.of(),
                "sourceType", library.getSourceType() != null ? library.getSourceType().name() : "",
                "sourceUrl", library.getSourceUrl() != null ? library.getSourceUrl() : "",
                "versions", versions.stream()
                        .map(v -> Map.of(
                                "id", v.getId(),
                                "version", v.getVersion(),
                                "isLatest", v.getIsLatest(),
                                "status", v.getStatus().name()
                        ))
                        .toList()
        );

        String jsonContent;
        try {
            jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            jsonContent = "{}";
        }

        String resourceUri = "library://" + libraryId;

        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        resourceUri,
                        "application/json",
                        jsonContent
                )
        ));
    }

    /**
     * 取得函式庫元資料（使用名稱）
     *
     * @param libraryName 函式庫名稱
     * @return 函式庫元資料（JSON 格式）
     */
    @McpResource(
            uri = "library://name/{libraryName}",
            name = "Library Metadata by Name",
            description = "透過名稱取得函式庫的元資料",
            mimeType = "application/json"
    )
    public ReadResourceResult getLibraryByNameResource(
            String libraryName
    ) {
        // 使用 resolveLibrary 來查找函式庫
        var resolved = libraryService.resolveLibrary(libraryName, null);
        var library = resolved.library();

        // 複用 getLibraryResource 的邏輯
        return getLibraryResource(library.getId());
    }
}
