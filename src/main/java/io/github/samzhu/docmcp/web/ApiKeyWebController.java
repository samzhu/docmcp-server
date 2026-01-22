package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.domain.enums.ApiKeyStatus;
import io.github.samzhu.docmcp.security.ApiKeyService;
import io.github.samzhu.docmcp.web.dto.ApiKeyDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API Key 管理頁面控制器
 * <p>
 * 提供 API Key 的列表、新增、撤銷頁面。
 * </p>
 */
@Controller
@RequestMapping("/api-keys")
public class ApiKeyWebController {

    private final ApiKeyService apiKeyService;

    public ApiKeyWebController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * API Key 列表頁面
     */
    @GetMapping
    public String list(Model model) {
        var apiKeys = apiKeyService.listKeys().stream()
                .map(ApiKeyDto::from)
                .toList();

        // 統計資料
        long totalCount = apiKeys.size();
        long activeCount = apiKeys.stream().filter(k -> k.status() == ApiKeyStatus.ACTIVE).count();
        long revokedCount = apiKeys.stream().filter(k -> k.status() == ApiKeyStatus.REVOKED).count();
        long expiredCount = apiKeys.stream().filter(k -> k.status() == ApiKeyStatus.EXPIRED).count();

        model.addAttribute("pageTitle", "API Keys");
        model.addAttribute("currentPage", "api-keys");
        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("revokedCount", revokedCount);
        model.addAttribute("expiredCount", expiredCount);

        return "api-keys/list";
    }

    /**
     * 新增 API Key（POST 請求）
     *
     * @param name      名稱
     * @param rateLimit 速率限制（可選）
     * @param expiresIn 過期時間（可選，天數）
     * @return 重導向到列表頁面
     */
    @PostMapping("/create")
    public String create(
            @RequestParam String name,
            @RequestParam(required = false) Integer rateLimit,
            @RequestParam(required = false) Integer expiresInDays,
            RedirectAttributes redirectAttributes) {

        try {
            // 計算過期時間
            OffsetDateTime expiresAt = null;
            if (expiresInDays != null && expiresInDays > 0) {
                expiresAt = OffsetDateTime.now().plusDays(expiresInDays);
            }

            // 產生金鑰
            var generated = apiKeyService.generateKey(name, "web-ui", expiresAt, rateLimit);

            // 將原始金鑰透過 flash attribute 傳遞（只顯示一次）
            redirectAttributes.addFlashAttribute("generatedKey", generated.rawKey());
            redirectAttributes.addFlashAttribute("generatedKeyName", generated.name());
            redirectAttributes.addFlashAttribute("successMessage", "API Key created successfully");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/api-keys";
    }

    /**
     * 撤銷 API Key
     *
     * @param id 金鑰 ID
     * @return 重導向到列表頁面
     */
    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            apiKeyService.revokeKey(id);
            redirectAttributes.addFlashAttribute("successMessage", "API Key revoked successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/api-keys";
    }
}
