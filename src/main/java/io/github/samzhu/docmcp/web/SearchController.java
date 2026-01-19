package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 搜尋頁面控制器
 * <p>
 * 提供搜尋頁面，允許用戶測試全文搜尋和語意搜尋功能。
 * </p>
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    private final LibraryService libraryService;

    public SearchController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * 搜尋頁面
     */
    @GetMapping
    public String search(@RequestParam(required = false) String q, Model model) {
        var libraries = libraryService.listLibraries(null);

        model.addAttribute("pageTitle", "Search");
        model.addAttribute("currentPage", "search");
        model.addAttribute("libraries", libraries);
        model.addAttribute("query", q);

        return "search/index";
    }
}
