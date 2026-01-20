package io.github.samzhu.docmcp.mcp.tool.retrieve;

import io.github.samzhu.docmcp.config.FeatureFlags;
import io.github.samzhu.docmcp.domain.model.Document;
import io.github.samzhu.docmcp.domain.model.LibraryVersion;
import io.github.samzhu.docmcp.mcp.dto.GetMigrationGuideResult;
import io.github.samzhu.docmcp.mcp.dto.GetMigrationGuideResult.MigrationGuideItem;
import io.github.samzhu.docmcp.repository.DocumentRepository;
import io.github.samzhu.docmcp.repository.LibraryVersionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * get_migration_guide MCP 工具
 * <p>
 * MCP Level 3 - Retrieve 工具，取得版本間的遷移指南。
 * 搜尋包含 migration、upgrade、breaking changes 等關鍵字的文件。
 * </p>
 */
@Component
public class GetMigrationGuideTool {

    private static final List<String> MIGRATION_KEYWORDS = List.of(
            "migration", "migrate", "upgrade", "upgrading",
            "breaking", "breaking-change", "breaking_change",
            "changelog", "release-notes", "release_notes"
    );

    private final FeatureFlags featureFlags;
    private final LibraryVersionRepository versionRepository;
    private final DocumentRepository documentRepository;

    public GetMigrationGuideTool(FeatureFlags featureFlags,
                                  LibraryVersionRepository versionRepository,
                                  DocumentRepository documentRepository) {
        this.featureFlags = featureFlags;
        this.versionRepository = versionRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * 取得版本遷移指南
     * <p>
     * 搜尋指定版本範圍內的遷移相關文件，包含升級指南、
     * 破壞性變更說明、版本發布說明等。
     * </p>
     *
     * @param libraryId   函式庫 ID（從 list_libraries 或 resolve_library 取得）
     * @param fromVersion 來源版本（目前使用的版本）
     * @param toVersion   目標版本（預計升級到的版本）
     * @return 遷移指南結果
     */
    @Tool(name = "get_migration_guide",
            description = """
                    取得版本遷移指南。

                    使用時機：
                    - 當需要從一個版本升級到另一個版本時
                    - 當想了解版本間的破壞性變更時
                    - 當需要查看升級步驟和注意事項時

                    回傳：遷移相關文件列表，包含升級指南、破壞性變更說明等。
                    """)
    public GetMigrationGuideResult getMigrationGuide(
            @ToolParam(description = "函式庫 ID（UUID 格式）")
            UUID libraryId,
            @ToolParam(description = "來源版本（目前使用的版本）")
            String fromVersion,
            @ToolParam(description = "目標版本（預計升級到的版本）")
            String toVersion
    ) {
        // 檢查功能是否啟用
        if (!featureFlags.isMigrationGuides()) {
            return new GetMigrationGuideResult(
                    libraryId,
                    fromVersion,
                    toVersion,
                    List.of(),
                    "Migration guides feature is currently disabled. " +
                            "Enable it by setting docmcp.features.migration-guides=true"
            );
        }

        // 驗證參數
        if (fromVersion == null || fromVersion.isBlank() ||
                toVersion == null || toVersion.isBlank()) {
            return new GetMigrationGuideResult(
                    libraryId,
                    fromVersion,
                    toVersion,
                    List.of(),
                    "Both fromVersion and toVersion are required"
            );
        }

        // 取得所有版本
        List<LibraryVersion> versions = versionRepository.findByLibraryId(libraryId);
        if (versions.isEmpty()) {
            return GetMigrationGuideResult.empty(libraryId, fromVersion, toVersion);
        }

        // 搜尋遷移相關文件
        List<MigrationGuideItem> guides = new ArrayList<>();

        for (LibraryVersion version : versions) {
            // 搜尋此版本的遷移相關文件
            List<Document> documents = documentRepository.findByVersionId(version.id());

            for (Document doc : documents) {
                String relevance = detectMigrationRelevance(doc);
                if (relevance != null) {
                    guides.add(new MigrationGuideItem(
                            doc.id(),
                            doc.title(),
                            doc.path(),
                            version.version(),
                            truncateContent(doc.content(), 300),
                            relevance
                    ));
                }
            }
        }

        // 依相關性排序（優先顯示 migration 相關文件）
        guides.sort(Comparator
                .comparing((MigrationGuideItem g) -> getRelevancePriority(g.relevance()))
                .thenComparing(MigrationGuideItem::version));

        return GetMigrationGuideResult.of(libraryId, fromVersion, toVersion, guides);
    }

    /**
     * 檢測文件是否與遷移相關
     */
    private String detectMigrationRelevance(Document doc) {
        String title = doc.title().toLowerCase();
        String path = doc.path().toLowerCase();
        String content = doc.content() != null ? doc.content().toLowerCase() : "";

        // 檢查標題和路徑
        for (String keyword : MIGRATION_KEYWORDS) {
            if (title.contains(keyword) || path.contains(keyword)) {
                return categorizeRelevance(keyword);
            }
        }

        // 檢查內容（只檢查前 2000 字元以提升效能）
        String contentPrefix = content.length() > 2000 ? content.substring(0, 2000) : content;
        for (String keyword : MIGRATION_KEYWORDS) {
            if (contentPrefix.contains(keyword)) {
                return categorizeRelevance(keyword);
            }
        }

        return null;
    }

    /**
     * 分類相關性
     */
    private String categorizeRelevance(String keyword) {
        return switch (keyword) {
            case "migration", "migrate" -> "migration";
            case "upgrade", "upgrading" -> "upgrade";
            case "breaking", "breaking-change", "breaking_change" -> "breaking-changes";
            case "changelog", "release-notes", "release_notes" -> "release-notes";
            default -> "other";
        };
    }

    /**
     * 取得相關性優先順序
     */
    private int getRelevancePriority(String relevance) {
        return switch (relevance) {
            case "migration" -> 1;
            case "upgrade" -> 2;
            case "breaking-changes" -> 3;
            case "release-notes" -> 4;
            default -> 5;
        };
    }

    /**
     * 截斷內容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
