package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.domain.enums.SourceType;
import io.github.samzhu.docmcp.service.LibraryService;
import io.github.samzhu.docmcp.service.SyncService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * 函式庫管理頁面控制器
 * <p>
 * 提供函式庫的列表、詳情、新增、編輯頁面。
 * </p>
 */
@Controller
@RequestMapping("/libraries")
public class LibraryController {

    private final LibraryService libraryService;
    private final SyncService syncService;

    public LibraryController(LibraryService libraryService, SyncService syncService) {
        this.libraryService = libraryService;
        this.syncService = syncService;
    }

    /**
     * 函式庫列表頁面
     */
    @GetMapping
    public String list(Model model) {
        var libraries = libraryService.listLibraries(null);

        model.addAttribute("pageTitle", "Libraries");
        model.addAttribute("currentPage", "libraries");
        model.addAttribute("libraries", libraries);

        return "libraries/list";
    }

    /**
     * 新增函式庫頁面
     */
    @GetMapping("/new")
    public String newLibrary(Model model) {
        model.addAttribute("pageTitle", "New Library");
        model.addAttribute("currentPage", "libraries");
        model.addAttribute("sourceTypes", SourceType.values());
        model.addAttribute("mode", "create");

        return "libraries/form";
    }

    /**
     * 函式庫詳情頁面
     *
     * @param id 函式庫 ID（TSID 格式）
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        var library = libraryService.getLibraryById(id);
        var versions = libraryService.getLibraryVersionsById(id);

        // 取得每個版本的最新同步狀態
        var syncStatuses = versions.stream()
                .map(v -> syncService.getLatestSyncHistory(v.getId()).orElse(null))
                .toList();

        model.addAttribute("pageTitle", library.getDisplayName());
        model.addAttribute("currentPage", "libraries");
        model.addAttribute("library", library);
        model.addAttribute("versions", versions);
        model.addAttribute("syncStatuses", syncStatuses);

        return "libraries/detail";
    }

    /**
     * 編輯函式庫頁面
     *
     * @param id 函式庫 ID（TSID 格式）
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model) {
        var library = libraryService.getLibraryById(id);

        model.addAttribute("pageTitle", "Edit " + library.getDisplayName());
        model.addAttribute("currentPage", "libraries");
        model.addAttribute("library", library);
        model.addAttribute("sourceTypes", SourceType.values());
        model.addAttribute("mode", "edit");

        return "libraries/form";
    }
}
