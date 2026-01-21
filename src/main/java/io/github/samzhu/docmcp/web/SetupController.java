package io.github.samzhu.docmcp.web;

import io.github.samzhu.docmcp.config.FeatureFlags;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * MCP 整合設定頁面控制器
 * <p>
 * 提供 AI 編程助手（Claude Code、VS Code、Cursor）的 MCP 設定指引，
 * 自動偵測伺服器 URL 並產生可直接複製的設定範例。
 * </p>
 */
@Controller
@RequestMapping("/setup")
public class SetupController {

    private final FeatureFlags featureFlags;

    public SetupController(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    /**
     * MCP 整合設定頁面
     * <p>
     * 自動偵測當前伺服器的 URL，產生各 AI 工具的設定範例。
     * </p>
     */
    @GetMapping
    public String setup(HttpServletRequest request, Model model) {
        // 頁面資訊
        model.addAttribute("pageTitle", "Setup");
        model.addAttribute("currentPage", "setup");

        // 自動偵測伺服器 URL
        String serverUrl = buildServerUrl(request);
        String mcpEndpoint = serverUrl + "/mcp";

        model.addAttribute("serverUrl", serverUrl);
        model.addAttribute("mcpEndpoint", mcpEndpoint);

        // 是否需要認證
        boolean requiresAuth = featureFlags.isApiKeyAuth();
        model.addAttribute("requiresAuth", requiresAuth);

        // 產生各工具的設定 JSON（轉義處理）
        model.addAttribute("claudeConfig", generateClaudeConfig(mcpEndpoint, requiresAuth));
        model.addAttribute("vscodeConfig", generateVSCodeConfig(mcpEndpoint, requiresAuth));
        model.addAttribute("cursorConfig", generateCursorConfig(mcpEndpoint, requiresAuth));

        // CLI 指令
        model.addAttribute("claudeCliCommand", generateClaudeCliCommand(mcpEndpoint, requiresAuth));

        return "setup/index";
    }

    /**
     * 建構伺服器 URL
     * <p>
     * 從 HttpServletRequest 自動偵測 scheme、host、port。
     * </p>
     */
    private String buildServerUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        // 標準 port 不顯示
        if ((scheme.equals("http") && serverPort == 80) ||
            (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        }

        return scheme + "://" + serverName + ":" + serverPort;
    }

    /**
     * 產生 Claude Code .mcp.json 設定
     */
    private String generateClaudeConfig(String mcpEndpoint, boolean requiresAuth) {
        if (requiresAuth) {
            return """
                {
                  "mcpServers": {
                    "docmcp": {
                      "type": "http",
                      "url": "%s",
                      "headers": {
                        "Authorization": "Bearer YOUR_API_KEY"
                      }
                    }
                  }
                }""".formatted(mcpEndpoint);
        }
        return """
            {
              "mcpServers": {
                "docmcp": {
                  "type": "http",
                  "url": "%s"
                }
              }
            }""".formatted(mcpEndpoint);
    }

    /**
     * 產生 VS Code settings.json 設定
     */
    private String generateVSCodeConfig(String mcpEndpoint, boolean requiresAuth) {
        if (requiresAuth) {
            return """
                {
                  "github.copilot.chat.mcp.servers": {
                    "docmcp": {
                      "type": "http",
                      "url": "%s",
                      "headers": {
                        "Authorization": "Bearer ${input:docmcp-api-key}"
                      }
                    }
                  }
                }""".formatted(mcpEndpoint);
        }
        return """
            {
              "github.copilot.chat.mcp.servers": {
                "docmcp": {
                  "type": "http",
                  "url": "%s"
                }
              }
            }""".formatted(mcpEndpoint);
    }

    /**
     * 產生 Cursor mcp.json 設定
     */
    private String generateCursorConfig(String mcpEndpoint, boolean requiresAuth) {
        if (requiresAuth) {
            return """
                {
                  "mcpServers": {
                    "docmcp": {
                      "type": "http",
                      "url": "%s",
                      "headers": {
                        "Authorization": "Bearer YOUR_API_KEY"
                      }
                    }
                  }
                }""".formatted(mcpEndpoint);
        }
        return """
            {
              "mcpServers": {
                "docmcp": {
                  "type": "http",
                  "url": "%s"
                }
              }
            }""".formatted(mcpEndpoint);
    }

    /**
     * 產生 Claude Code CLI 指令
     */
    private String generateClaudeCliCommand(String mcpEndpoint, boolean requiresAuth) {
        if (requiresAuth) {
            return "claude mcp add --transport http docmcp " + mcpEndpoint +
                   " --header \"Authorization: Bearer YOUR_API_KEY\"";
        }
        return "claude mcp add --transport http docmcp " + mcpEndpoint;
    }
}
