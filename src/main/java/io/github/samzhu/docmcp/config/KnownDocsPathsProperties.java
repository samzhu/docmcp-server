package io.github.samzhu.docmcp.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 已知函式庫文件路徑配置
 * <p>
 * 維護已知函式庫的預設文件路徑對應表，支援版本範圍配置。
 * 當用戶同步 GitHub Release 時，系統會根據 owner/repo 和版本號自動帶入對應的文件路徑。
 * </p>
 * <p>
 * 配置範例:
 * <pre>
 * docmcp:
 *   known-docs-paths:
 *     spring-projects/spring-boot:
 *       default: "documentation/spring-boot-docs/src/docs/antora"  # 4.x 預設
 *       versions:
 *         "3.*": "spring-boot-project/spring-boot-docs/src/docs/antora"  # 3.x 特例
 *     facebook/react: "docs"  # 簡單格式（無版本差異）
 * </pre>
 * </p>
 */
@ConfigurationProperties(prefix = "docmcp")
public class KnownDocsPathsProperties {

    private static final Logger log = LoggerFactory.getLogger(KnownDocsPathsProperties.class);

    /**
     * 已知函式庫的文件路徑對應表
     * Key: owner/repo（如 spring-projects/spring-boot）
     * Value: DocsPathConfig 或 String（簡單格式）
     */
    private Map<String, Object> knownDocsPaths = new HashMap<>();

    /**
     * 預設文件路徑（當找不到對應時使用）
     */
    private static final String DEFAULT_DOCS_PATH = "docs";

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        log.info("KnownDocsPathsProperties loaded with {} entries: {}", knownDocsPaths.size(), knownDocsPaths.keySet());
        // 詳細顯示每個配置的內容，方便除錯
        for (Map.Entry<String, Object> entry : knownDocsPaths.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> configMap = (Map<String, Object>) value;
                log.info("  {} -> default: {}, versions: {}",
                    entry.getKey(),
                    configMap.get("default"),
                    configMap.get("versions"));
            } else {
                log.info("  {} -> {}", entry.getKey(), value);
            }
        }
    }

    public Map<String, Object> getKnownDocsPaths() {
        return knownDocsPaths;
    }

    public void setKnownDocsPaths(Map<String, Object> knownDocsPaths) {
        this.knownDocsPaths = knownDocsPaths;
    }

    /**
     * 根據 owner/repo 取得預設文件路徑（不考慮版本）
     *
     * @param ownerRepo owner/repo 字串（如 spring-projects/spring-boot）
     * @return 對應的預設文件路徑
     */
    public String getDocsPath(String ownerRepo) {
        return getDocsPath(ownerRepo, null);
    }

    /**
     * 根據 owner/repo 和版本號取得文件路徑
     * <p>
     * 支援兩種配置格式：
     * 1. 簡單格式：直接是路徑字串
     * 2. 版本範圍格式：包含 default 和 versions 的 Map
     * </p>
     *
     * @param ownerRepo owner/repo 字串（如 spring-projects/spring-boot）
     * @param version   版本號（如 4.0.1、3.4.0），可為 null
     * @return 對應的文件路徑，若找不到則回傳預設值 "docs"
     */
    public String getDocsPath(String ownerRepo, String version) {
        // Spring Boot relaxed binding 會移除斜線，所以查詢時也要 normalize
        String normalizedKey = normalizeKey(ownerRepo);

        Object config = findConfigByNormalizedKey(normalizedKey);
        if (config == null) {
            log.debug("No docs path config found for {}, using default: {}", ownerRepo, DEFAULT_DOCS_PATH);
            return DEFAULT_DOCS_PATH;
        }

        return resolveDocsPath(config, version, ownerRepo);
    }

    /**
     * 檢查是否有該函式庫的已知路徑
     *
     * @param ownerRepo owner/repo 字串
     * @return 是否有對應的路徑
     */
    public boolean hasKnownPath(String ownerRepo) {
        String normalizedKey = normalizeKey(ownerRepo);
        return findConfigByNormalizedKey(normalizedKey) != null;
    }

    /**
     * 將 key normalize（移除斜線並轉小寫）
     */
    private String normalizeKey(String key) {
        return key.replace("/", "").toLowerCase();
    }

    /**
     * 透過 normalized key 查找配置
     */
    private Object findConfigByNormalizedKey(String normalizedKey) {
        for (Map.Entry<String, Object> entry : knownDocsPaths.entrySet()) {
            if (normalizeKey(entry.getKey()).equals(normalizedKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析文件路徑配置
     *
     * @param config    配置物件（String 或 Map）
     * @param version   版本號
     * @param ownerRepo owner/repo（用於 log）
     * @return 解析後的文件路徑
     */
    @SuppressWarnings("unchecked")
    private String resolveDocsPath(Object config, String version, String ownerRepo) {
        // 簡單格式：直接是路徑字串
        if (config instanceof String) {
            log.debug("Found simple docs path for {}: {}", ownerRepo, config);
            return (String) config;
        }

        // 版本範圍格式：Map 結構
        if (config instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) config;
            String defaultPath = (String) configMap.get("default");

            // 如果沒有提供版本號，直接返回 default
            if (version == null || version.isBlank()) {
                log.debug("No version provided for {}, using default path: {}", ownerRepo, defaultPath);
                return defaultPath != null ? defaultPath : DEFAULT_DOCS_PATH;
            }

            // 檢查版本範圍配置
            // 注意：Spring Boot relaxed binding 可能將 "3.*" 轉成 Integer 3
            Object versionsObj = configMap.get("versions");
            if (versionsObj instanceof Map) {
                Map<?, ?> versions = (Map<?, ?>) versionsObj;
                String matchedPath = matchVersionPattern(versions, version);
                if (matchedPath != null) {
                    log.debug("Found version-specific path for {} v{}: {}", ownerRepo, version, matchedPath);
                    return matchedPath;
                }
            }

            // 沒有匹配的版本範圍，使用 default
            log.debug("No version pattern matched for {} v{}, using default: {}", ownerRepo, version, defaultPath);
            return defaultPath != null ? defaultPath : DEFAULT_DOCS_PATH;
        }

        log.warn("Unknown config type for {}: {}", ownerRepo, config.getClass());
        return DEFAULT_DOCS_PATH;
    }

    /**
     * 匹配版本模式
     * <p>
     * 支援的模式：
     * - "3.*" 或 "3" (integer) 匹配所有 3.x.x 版本（Spring Boot relaxed binding 會將 "3.*" 轉成 3）
     * - "3.4.*" 匹配所有 3.4.x 版本
     * - "3.4.0" 精確匹配
     * </p>
     *
     * @param versions 版本模式對應表（key 可能是 String 或 Integer）
     * @param version  實際版本號
     * @return 匹配的路徑，若無匹配則回傳 null
     */
    private String matchVersionPattern(Map<?, ?> versions, String version) {
        // 移除版本號前綴 'v'（如 v4.0.1 -> 4.0.1）
        String normalizedVersion = version.startsWith("v") ? version.substring(1) : version;

        log.debug("Trying to match version '{}' against patterns: {}", normalizedVersion, versions.keySet());

        for (Map.Entry<?, ?> entry : versions.entrySet()) {
            Object patternKey = entry.getKey();
            String pattern = String.valueOf(patternKey);
            Object pathValue = entry.getValue();
            String path = String.valueOf(pathValue);

            boolean matches = matchesPattern(pattern, normalizedVersion);
            log.debug("  Pattern '{}' (type: {}) matches '{}': {}",
                    pattern, patternKey.getClass().getSimpleName(), normalizedVersion, matches);
            if (matches) {
                return path;
            }
        }
        return null;
    }

    /**
     * 檢查版本號是否匹配模式
     * <p>
     * 支援的匹配邏輯：
     * - "3" 匹配主版本 3.x.x（Spring Boot 會將 "3.*" 轉成 3）
     * - "3.*" 匹配 3.x.x
     * - "3.4.*" 匹配 3.4.x
     * - "3.4.0" 精確匹配
     * </p>
     *
     * @param pattern 模式（如 "3.*" 或 "3"）
     * @param version 版本號（如 "3.4.0"）
     * @return 是否匹配
     */
    private boolean matchesPattern(String pattern, String version) {
        // 萬用字元匹配（如 "3.*" 匹配 "3.4.0"）
        if (pattern.contains("*")) {
            String prefix = pattern.replace("*", "");
            return version.startsWith(prefix);
        }

        // 純數字模式（如 "3"）- 表示主版本號匹配
        // 這是 Spring Boot relaxed binding 將 "3.*" 轉成 Integer 3 的結果
        if (pattern.matches("\\d+")) {
            String majorVersionPrefix = pattern + ".";
            return version.startsWith(majorVersionPrefix);
        }

        // 精確匹配
        return pattern.equals(version);
    }
}
