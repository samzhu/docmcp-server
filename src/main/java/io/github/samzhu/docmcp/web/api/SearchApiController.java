package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SearchService;
import io.github.samzhu.docmcp.web.dto.SearchResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜尋 REST API
 * <p>
 * 提供文件搜尋功能，支援全文搜尋、語意搜尋和混合搜尋。
 * </p>
 */
@RestController
@RequestMapping("/api/search")
public class SearchApiController {

    private static final double DEFAULT_SEMANTIC_THRESHOLD = 0.5;

    private final SearchService searchService;
    private final LibraryService libraryService;

    public SearchApiController(SearchService searchService, LibraryService libraryService) {
        this.searchService = searchService;
        this.libraryService = libraryService;
    }

    /**
     * 搜尋文件
     *
     * @param query     搜尋查詢
     * @param libraryId 函式庫 ID（可選）
     * @param version   版本（可選）
     * @param mode      搜尋模式：fulltext、semantic、hybrid（預設）
     * @param limit     結果數量上限（預設 10）
     * @return 搜尋結果
     */
    @GetMapping
    public SearchResultDto search(
            @RequestParam String query,
            @RequestParam(required = false) String libraryId,
            @RequestParam(required = false) String version,
            @RequestParam(defaultValue = "hybrid") String mode,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (query == null || query.isBlank()) {
            return new SearchResultDto(query, mode, 0, List.of());
        }

        // 如果沒有指定 libraryId，需要從所有函式庫搜尋（目前實作只支援單一函式庫）
        // libraryId 現為 String（TSID 格式，13 字元）
        if (libraryId == null || libraryId.isBlank()) {
            // 取得第一個函式庫作為預設
            var libraries = libraryService.listLibraries(null);
            if (libraries.isEmpty()) {
                return new SearchResultDto(query, mode, 0, List.of());
            }
            libraryId = libraries.getFirst().getId();
        }

        List<SearchResultItem> results = switch (mode.toLowerCase()) {
            case "fulltext" -> searchService.fullTextSearch(libraryId, version, query, limit);
            case "semantic" -> searchService.semanticSearch(libraryId, version, query, limit, DEFAULT_SEMANTIC_THRESHOLD);
            case "hybrid" -> searchService.hybridSearch(libraryId, version, query, limit);
            default -> throw new IllegalArgumentException("不支援的搜尋模式: " + mode);
        };

        return SearchResultDto.from(query, mode, results);
    }
}
