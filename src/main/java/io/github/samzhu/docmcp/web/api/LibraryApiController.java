package io.github.samzhu.docmcp.web.api;

import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SyncService;
import io.github.samzhu.docmcp.web.dto.CreateLibraryRequest;
import io.github.samzhu.docmcp.web.dto.LibraryVersionDto;
import io.github.samzhu.docmcp.web.dto.SyncHistoryDto;
import io.github.samzhu.docmcp.web.dto.TriggerSyncRequest;
import io.github.samzhu.docmcp.web.dto.UpdateLibraryRequest;
import io.github.samzhu.docmcp.web.dto.WebLibraryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 函式庫 REST API
 * <p>
 * 提供函式庫的 CRUD 操作和同步觸發 API。
 * </p>
 */
@RestController
@RequestMapping("/api/libraries")
public class LibraryApiController {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/([^/]+)/([^/]+)(?:/.*)?$"
    );

    private final LibraryService libraryService;
    private final SyncService syncService;

    public LibraryApiController(LibraryService libraryService, SyncService syncService) {
        this.libraryService = libraryService;
        this.syncService = syncService;
    }

    /**
     * 列出所有函式庫
     *
     * @param category 分類篩選（可選）
     * @return 函式庫列表
     */
    @GetMapping
    public List<WebLibraryDto> listLibraries(@RequestParam(required = false) String category) {
        return libraryService.listLibraries(category).stream()
                .map(WebLibraryDto::from)
                .toList();
    }

    /**
     * 建立新函式庫
     *
     * @param request 建立請求
     * @return 建立的函式庫
     */
    @PostMapping
    public ResponseEntity<WebLibraryDto> createLibrary(@RequestBody @Valid CreateLibraryRequest request) {
        Library library = libraryService.createLibrary(
                request.name(),
                request.displayName(),
                request.description(),
                request.sourceType(),
                request.sourceUrl(),
                request.category(),
                request.tags()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WebLibraryDto.from(library));
    }

    /**
     * 取得單一函式庫
     *
     * @param id 函式庫 ID
     * @return 函式庫資訊
     */
    @GetMapping("/{id}")
    public WebLibraryDto getLibrary(@PathVariable UUID id) {
        return WebLibraryDto.from(libraryService.getLibraryById(id));
    }

    /**
     * 更新函式庫
     *
     * @param id      函式庫 ID
     * @param request 更新請求
     * @return 更新後的函式庫
     */
    @PutMapping("/{id}")
    public WebLibraryDto updateLibrary(@PathVariable UUID id,
                                        @RequestBody @Valid UpdateLibraryRequest request) {
        Library library = libraryService.updateLibrary(
                id,
                request.displayName(),
                request.description(),
                request.sourceType(),
                request.sourceUrl(),
                request.category(),
                request.tags()
        );
        return WebLibraryDto.from(library);
    }

    /**
     * 刪除函式庫
     *
     * @param id 函式庫 ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLibrary(@PathVariable UUID id) {
        libraryService.deleteLibrary(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 取得函式庫的所有版本
     *
     * @param id 函式庫 ID
     * @return 版本列表
     */
    @GetMapping("/{id}/versions")
    public List<LibraryVersionDto> listVersions(@PathVariable UUID id) {
        return libraryService.getLibraryVersionsById(id).stream()
                .map(LibraryVersionDto::from)
                .toList();
    }

    /**
     * 觸發同步
     *
     * @param id      函式庫 ID
     * @param request 同步請求（包含版本）
     * @return 同步歷史
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncHistoryDto> triggerSync(@PathVariable UUID id,
                                                       @RequestBody @Valid TriggerSyncRequest request) {
        Library library = libraryService.getLibraryById(id);

        // 解析 GitHub URL
        Matcher matcher = GITHUB_URL_PATTERN.matcher(library.sourceUrl());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("無效的 GitHub URL: " + library.sourceUrl());
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        // 取得或建立版本
        LibraryVersion version = libraryService.resolveLibrary(library.name(), request.version()).version();

        // 觸發同步
        SyncHistory syncHistory = syncService.syncFromGitHub(
                version.id(),
                owner,
                repo,
                version.docsPath() != null ? version.docsPath() : "docs",
                "v" + request.version()
        ).join();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncHistoryDto.from(syncHistory));
    }
}
