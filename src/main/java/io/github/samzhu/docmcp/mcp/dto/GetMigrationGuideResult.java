package io.github.samzhu.docmcp.mcp.dto;

import java.util.List;
import java.util.UUID;

/**
 * get_migration_guide 工具回傳結果
 * <p>
 * 回傳版本間的遷移指南資訊。
 * </p>
 *
 * @param libraryId   函式庫 ID
 * @param fromVersion 來源版本
 * @param toVersion   目標版本
 * @param guides      遷移指南列表
 * @param message     訊息（當無遷移指南時提供說明）
 */
public record GetMigrationGuideResult(
        UUID libraryId,
        String fromVersion,
        String toVersion,
        List<MigrationGuideItem> guides,
        String message
) {

    /**
     * 遷移指南項目
     *
     * @param documentId 文件 ID
     * @param title      標題
     * @param path       文件路徑
     * @param version    對應版本
     * @param excerpt    內容摘要
     * @param relevance  相關性（migration/upgrade/breaking-changes）
     */
    public record MigrationGuideItem(
            UUID documentId,
            String title,
            String path,
            String version,
            String excerpt,
            String relevance
    ) {}

    /**
     * 建立無結果的回應
     */
    public static GetMigrationGuideResult empty(UUID libraryId, String fromVersion, String toVersion) {
        return new GetMigrationGuideResult(
                libraryId,
                fromVersion,
                toVersion,
                List.of(),
                String.format("No migration guides found from version %s to %s", fromVersion, toVersion)
        );
    }

    /**
     * 建立有結果的回應
     */
    public static GetMigrationGuideResult of(UUID libraryId, String fromVersion, String toVersion,
                                              List<MigrationGuideItem> guides) {
        return new GetMigrationGuideResult(
                libraryId,
                fromVersion,
                toVersion,
                guides,
                guides.isEmpty()
                        ? String.format("No migration guides found from version %s to %s", fromVersion, toVersion)
                        : String.format("Found %d migration guide(s) from version %s to %s",
                                guides.size(), fromVersion, toVersion)
        );
    }
}
