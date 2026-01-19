package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.repository.DocumentChunkRepository;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryRepository;
import io.github.samzhu.docmcp.service.SyncService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard 頁面控制器
 * <p>
 * 提供系統概覽頁面，顯示函式庫統計和近期同步狀態。
 * </p>
 */
@Controller
public class DashboardController {

    private final LibraryRepository libraryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final SyncService syncService;

    public DashboardController(LibraryRepository libraryRepository,
                               DocumentRepository documentRepository,
                               DocumentChunkRepository chunkRepository,
                               SyncService syncService) {
        this.libraryRepository = libraryRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.syncService = syncService;
    }

    /**
     * 首頁 - 重導向到 Dashboard
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    /**
     * Dashboard 頁面
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 取得統計資料
        long libraryCount = libraryRepository.count();
        long documentCount = documentRepository.count();
        long chunkCount = chunkRepository.count();

        // 取得近期同步歷史
        var recentSyncs = syncService.getSyncHistory(null, 5);

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("libraryCount", libraryCount);
        model.addAttribute("documentCount", documentCount);
        model.addAttribute("chunkCount", chunkCount);
        model.addAttribute("recentSyncs", recentSyncs);

        return "dashboard";
    }
}
