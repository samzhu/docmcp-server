package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.service.DocumentService;
import io.github.samzhu.docmcp.service.LibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * 文件瀏覽頁面控制器
 * <p>
 * 提供文件列表和詳情頁面。
 * </p>
 */
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final LibraryService libraryService;

    public DocumentController(DocumentService documentService,
                               DocumentRepository documentRepository,
                               LibraryService libraryService) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.libraryService = libraryService;
    }

    /**
     * 文件列表頁面
     *
     * @param versionId 版本 ID（可選，若未提供則重導向至 Libraries 頁面）
     * @param model     Model
     * @return 視圖名稱
     */
    @GetMapping
    public String list(@RequestParam(required = false) UUID versionId, Model model) {
        // 若未提供 versionId，重導向至 Libraries 頁面讓用戶選擇版本
        if (versionId == null) {
            return "redirect:/libraries";
        }

        var version = libraryService.getVersionById(versionId);
        var library = libraryService.getLibraryById(version.libraryId());
        var documents = documentRepository.findByVersionIdOrderByPathAsc(versionId);

        model.addAttribute("pageTitle", "Documents - " + library.displayName() + " " + version.version());
        model.addAttribute("currentPage", "documents");
        model.addAttribute("library", library);
        model.addAttribute("version", version);
        model.addAttribute("documents", documents);

        return "documents/list";
    }

    /**
     * 文件詳情頁面
     *
     * @param id    文件 ID
     * @param model Model
     * @return 視圖名稱
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Document document = documentService.getDocument(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        LibraryVersion version = libraryService.getVersionById(document.versionId());
        var library = libraryService.getLibraryById(version.libraryId());

        model.addAttribute("pageTitle", document.title());
        model.addAttribute("currentPage", "documents");
        model.addAttribute("library", library);
        model.addAttribute("version", version);
        model.addAttribute("document", document);

        return "documents/detail";
    }
}
