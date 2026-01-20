package io.github.samzhu.docmcp.mcp.resource;

import io.github.samzhu.docmcp.domain.exception.LibraryNotFoundException;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.LibraryService;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件內容資源提供者
 * <p>
 * 提供 MCP Resource 存取文件內容。
 * URI 格式: docs://{libraryName}/{version}/{path}
 * </p>
 */
@Component
public class DocResourceProvider {

    private final LibraryService libraryService;
    private final DocumentRepository documentRepository;

    public DocResourceProvider(LibraryService libraryService,
                               DocumentRepository documentRepository) {
        this.libraryService = libraryService;
        this.documentRepository = documentRepository;
    }

    /**
     * 取得文件內容資源
     * <p>
     * 透過 URI 模板存取文件內容。
     * </p>
     *
     * @param libraryName 函式庫名稱
     * @param version     版本號
     * @param path        文件路徑
     * @return 文件內容
     */
    @McpResource(
            uri = "docs://{libraryName}/{version}/{path}",
            name = "Documentation Content",
            description = "取得指定函式庫版本的文件內容",
            mimeType = "text/markdown"
    )
    public ReadResourceResult getDocResource(
            String libraryName,
            String version,
            String path
    ) {
        // 解析函式庫和版本
        var resolved = libraryService.resolveLibrary(libraryName, version);
        var libraryVersion = resolved.version();

        // 查找文件
        var document = documentRepository.findByVersionIdAndPath(libraryVersion.id(), path)
                .orElseThrow(() -> new LibraryNotFoundException(
                        "文件不存在: " + path + " (version: " + version + ")"));

        // 建立 URI
        String resourceUri = String.format("docs://%s/%s/%s", libraryName, version, path);

        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        resourceUri,
                        "text/markdown",
                        document.content()
                )
        ));
    }

    /**
     * 取得文件內容（使用最新版本）
     *
     * @param libraryName 函式庫名稱
     * @param path        文件路徑
     * @return 文件內容
     */
    @McpResource(
            uri = "docs://{libraryName}/latest/{path}",
            name = "Latest Documentation Content",
            description = "取得指定函式庫最新版本的文件內容",
            mimeType = "text/markdown"
    )
    public ReadResourceResult getLatestDocResource(
            String libraryName,
            String path
    ) {
        // 解析函式庫（使用最新版本）
        var resolved = libraryService.resolveLibrary(libraryName, null);
        var libraryVersion = resolved.version();

        // 查找文件
        var document = documentRepository.findByVersionIdAndPath(libraryVersion.id(), path)
                .orElseThrow(() -> new LibraryNotFoundException(
                        "文件不存在: " + path));

        // 建立 URI
        String resourceUri = String.format("docs://%s/latest/%s", libraryName, path);

        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        resourceUri,
                        "text/markdown",
                        document.content()
                )
        ));
    }
}
