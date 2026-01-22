package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.domain.enums.SyncStatus;
import io.github.samzhu.docmcp.domain.model.Library;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.domain.model.SyncHistory;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import io.github.samzhu.docmcp.service.SyncService;
import io.github.samzhu.docmcp.web.dto.SyncHistoryViewDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 同步狀態頁面控制器
 * <p>
 * 提供同步歷史列表、詳情頁面。
 * </p>
 */
@Controller
@RequestMapping("/sync")
public class SyncController {

    private final SyncService syncService;
    private final LibraryVersionRepository versionRepository;
    private final LibraryRepository libraryRepository;

    public SyncController(SyncService syncService,
                          LibraryVersionRepository versionRepository,
                          LibraryRepository libraryRepository) {
        this.syncService = syncService;
        this.versionRepository = versionRepository;
        this.libraryRepository = libraryRepository;
    }

    /**
     * 同步歷史列表頁面
     *
     * @param status    狀態篩選（可選）
     * @param libraryId 函式庫 ID 篩選（可選）
     * @param limit     結果數量上限（預設 50）
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) SyncStatus status,
            @RequestParam(required = false) String libraryId,
            @RequestParam(defaultValue = "50") int limit,
            Model model) {

        // 取得同步歷史
        List<SyncHistory> histories = syncService.getSyncHistory(null, limit);

        // 建立版本 ID 到 LibraryVersion 的對應表（ID 為 String，TSID 格式）
        Map<String, LibraryVersion> versionMap = histories.stream()
                .map(SyncHistory::getVersionId)
                .distinct()
                .map(versionRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(LibraryVersion::getId, Function.identity()));

        // 建立函式庫 ID 到 Library 的對應表（ID 為 String，TSID 格式）
        Map<String, Library> libraryMap = StreamSupport.stream(libraryRepository.findAll().spliterator(), false)
                .collect(Collectors.toMap(Library::getId, Function.identity()));

        // 轉換為 ViewDto
        List<SyncHistoryViewDto> syncHistories = histories.stream()
                .map(h -> {
                    LibraryVersion version = versionMap.get(h.getVersionId());
                    Library library = version != null ? libraryMap.get(version.getLibraryId()) : null;
                    return SyncHistoryViewDto.from(h, version, library);
                })
                // 狀態篩選
                .filter(h -> status == null || h.status() == status)
                // 函式庫篩選
                .filter(h -> libraryId == null || (h.libraryId() != null && h.libraryId().equals(libraryId)))
                .toList();

        // 取得所有函式庫（供篩選下拉選單使用）
        List<Library> libraries = libraryRepository.findAll();

        // 統計資料
        long totalCount = syncHistories.size();
        long successCount = syncHistories.stream().filter(h -> h.status() == SyncStatus.SUCCESS).count();
        long failedCount = syncHistories.stream().filter(h -> h.status() == SyncStatus.FAILED).count();
        long runningCount = syncHistories.stream().filter(h -> h.status() == SyncStatus.RUNNING).count();

        model.addAttribute("pageTitle", "Sync Status");
        model.addAttribute("currentPage", "sync");
        model.addAttribute("syncHistories", syncHistories);
        model.addAttribute("libraries", libraries);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedLibraryId", libraryId);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("successCount", successCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("runningCount", runningCount);

        return "sync/list";
    }

    /**
     * 同步詳情頁面
     *
     * @param id 同步 ID
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        var historyOpt = syncService.getSyncStatus(id);

        if (historyOpt.isEmpty()) {
            return "redirect:/sync";
        }

        SyncHistory history = historyOpt.get();

        // 取得版本資訊
        LibraryVersion version = versionRepository.findById(history.getVersionId()).orElse(null);

        // 取得函式庫資訊
        Library library = null;
        if (version != null) {
            library = libraryRepository.findById(version.getLibraryId()).orElse(null);
        }

        SyncHistoryViewDto syncDetail = SyncHistoryViewDto.from(history, version, library);

        model.addAttribute("pageTitle", "Sync Detail");
        model.addAttribute("currentPage", "sync");
        model.addAttribute("syncDetail", syncDetail);
        model.addAttribute("library", library);
        model.addAttribute("version", version);

        return "sync/detail";
    }
}
