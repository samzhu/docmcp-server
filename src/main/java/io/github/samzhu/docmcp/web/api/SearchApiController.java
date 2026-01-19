package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.mcp.dto.SearchResultItem;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SearchService;
import io.github.samzhu.docmcp.web.dto.SearchResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
            @RequestParam(required = false) UUID libraryId,
            @RequestParam(required = false) String version,
            @RequestParam(defaultValue = "hybrid") String mode,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (query == null || query.isBlank()) {
            return new SearchResultDto(query, mode, 0, List.of());
        }

        // 如果沒有指定 libraryId，需要從所有函式庫搜尋（目前實作只支援單一函式庫）
        if (libraryId == null) {
            // 取得第一個函式庫作為預設
            var libraries = libraryService.listLibraries(null);
            if (libraries.isEmpty()) {
                return new SearchResultDto(query, mode, 0, List.of());
            }
            libraryId = libraries.getFirst().id();
        }

        List<SearchResultItem> results = switch (mode.toLowerCase()) {
            case "fulltext" -> searchService.fullTextSearch(libraryId, version, query, limit);
            case "semantic" -> searchService.semanticSearch(libraryId, version, query, limit, DEFAULT_SEMANTIC_THRESHOLD);
            case "hybrid" -> performHybridSearch(libraryId, version, query, limit);
            default -> throw new IllegalArgumentException("不支援的搜尋模式: " + mode);
        };

        return SearchResultDto.from(query, mode, results);
    }

    /**
     * 執行混合搜尋（結合全文和語意搜尋）
     */
    private List<SearchResultItem> performHybridSearch(UUID libraryId, String version, String query, int limit) {
        // 同時執行全文搜尋和語意搜尋
        List<SearchResultItem> fulltextResults = searchService.fullTextSearch(libraryId, version, query, limit);
        List<SearchResultItem> semanticResults = searchService.semanticSearch(libraryId, version, query, limit, DEFAULT_SEMANTIC_THRESHOLD);

        // 合併結果，去除重複
        List<SearchResultItem> combined = new ArrayList<>(fulltextResults);
        for (SearchResultItem item : semanticResults) {
            boolean exists = combined.stream()
                    .anyMatch(existing -> existing.documentId().equals(item.documentId()) &&
                            (existing.chunkId() == null && item.chunkId() == null ||
                             existing.chunkId() != null && existing.chunkId().equals(item.chunkId())));
            if (!exists) {
                combined.add(item);
            }
        }

        // 依分數排序並限制數量
        return combined.stream()
                .sorted(Comparator.comparingDouble(SearchResultItem::score).reversed())
                .limit(limit)
                .toList();
    }
}
